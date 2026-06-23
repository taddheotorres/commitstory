package com.thiz.commitstory.service;

import com.thiz.commitstory.entity.CommitEntry;
import com.thiz.commitstory.entity.GitRepo;
import com.thiz.commitstory.repository.CommitEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoteSyncServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CommitEntryRepository commitEntryRepository;

    @InjectMocks
    private RemoteSyncService remoteSyncService;

    private UUID repoId;
    private GitRepo repo;

    @BeforeEach
    void setUp() {
        repoId = UUID.randomUUID();
        repo = new GitRepo();
        repo.setId(repoId);
        repo.setName("test-repo");
        repo.setRemoteUrl("https://github.com/testuser/testrepo");
    }

    @Test
    void should_sync_commits_from_github_successfully() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        Map<String, Object> mockCommit1 = Map.of(
                "sha", "abc123def456",
                "commit", Map.of(
                        "author", Map.of(
                                "name", "Alice",
                                "email", "alice@example.com",
                                "date", now.toString()
                        ),
                        "message", "feat: add feature"
                ),
                "files", List.of(
                        Map.of("filename", "src/main.ts", "additions", 10, "deletions", 2)
                )
        );

        when(commitEntryRepository.existsByRepoIdAndSha(repoId, "abc123def456")).thenReturn(false);
        when(commitEntryRepository.save(any(CommitEntry.class))).thenReturn(new CommitEntry());

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<Map<String, Object>>>() {})))
                .thenReturn(new ResponseEntity<>(List.of(mockCommit1), HttpStatus.OK));

        int imported = remoteSyncService.syncFromGitHub(repo);

        assertEquals(1, imported);
        verify(commitEntryRepository).save(any(CommitEntry.class));
    }

    @Test
    void should_skip_existing_commits() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        Map<String, Object> mockCommit = Map.of(
                "sha", "abc123def456",
                "commit", Map.of(
                        "author", Map.of(
                                "name", "Alice",
                                "email", "alice@example.com",
                                "date", now.toString()
                        ),
                        "message", "feat: add feature"
                )
        );

        when(commitEntryRepository.existsByRepoIdAndSha(repoId, "abc123def456")).thenReturn(true);

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<Map<String, Object>>>() {})))
                .thenReturn(new ResponseEntity<>(List.of(mockCommit), HttpStatus.OK));

        int imported = remoteSyncService.syncFromGitHub(repo);

        assertEquals(0, imported);
        verify(commitEntryRepository, never()).save(any(CommitEntry.class));
    }

    @Test
    void should_handle_pagination() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        Map<String, Object> mockCommit = Map.of(
                "sha", "abc123def456",
                "commit", Map.of(
                        "author", Map.of(
                                "name", "Alice",
                                "email", "alice@example.com",
                                "date", now.toString()
                        ),
                        "message", "feat: add feature"
                )
        );

        when(commitEntryRepository.existsByRepoIdAndSha(repoId, "abc123def456")).thenReturn(false);
        when(commitEntryRepository.save(any(CommitEntry.class))).thenReturn(new CommitEntry());

        // First page with next link, second page without
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<Map<String, Object>>>() {})))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);
                    if (url.contains("page=1")) {
                        var response = new ResponseEntity<>(List.of(mockCommit), HttpStatus.OK);
                        response.getHeaders().set("Link", "<next>; rel=\"next\"");
                        return response;
                    } else {
                        return new ResponseEntity<>(List.of(), HttpStatus.OK);
                    }
                });

        int imported = remoteSyncService.syncFromGitHub(repo);

        assertEquals(1, imported);
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<Map<String, Object>>>() {}));
    }

    @Test
    void should_throw_on_invalid_github_url() {
        repo.setRemoteUrl("https://gitlab.com/user/repo");

        assertThrows(IllegalArgumentException.class, () -> remoteSyncService.syncFromGitHub(repo));
    }

    @Test
    void should_handle_github_url_with_git_extension() {
        repo.setRemoteUrl("https://github.com/testuser/testrepo.git");

        when(commitEntryRepository.existsByRepoIdAndSha(any(), any())).thenReturn(false);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<Map<String, Object>>>() {})))
                .thenReturn(new ResponseEntity<>(List.of(), HttpStatus.OK));

        int imported = remoteSyncService.syncFromGitHub(repo);

        assertEquals(0, imported);
    }

    @Test
    void should_handle_ssh_github_url() {
        repo.setRemoteUrl("git@github.com:testuser/testrepo.git");

        when(commitEntryRepository.existsByRepoIdAndSha(any(), any())).thenReturn(false);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<Map<String, Object>>>() {})))
                .thenReturn(new ResponseEntity<>(List.of(), HttpStatus.OK));

        int imported = remoteSyncService.syncFromGitHub(repo);

        assertEquals(0, imported);
    }

    @Test
    void should_return_zero_on_empty_response() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<Map<String, Object>>>() {})))
                .thenReturn(new ResponseEntity<>(List.of(), HttpStatus.OK));

        int imported = remoteSyncService.syncFromGitHub(repo);

        assertEquals(0, imported);
        verify(commitEntryRepository, never()).save(any(CommitEntry.class));
    }

    @Test
    void should_use_github_token_when_available() {
        ReflectionTestUtils.setField(remoteSyncService, "githubToken", "test-token");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<Map<String, Object>>>() {})))
                .thenReturn(new ResponseEntity<>(List.of(), HttpStatus.OK));

        remoteSyncService.syncFromGitHub(repo);

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<Map<String, Object>>>() {}));
    }

    @Test
    void should_handle_null_commit_data() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        Map<String, Object> mockCommit = Map.of(
                "sha", "abc123def456",
                "commit", Map.of(
                        "author", Map.of(
                                "name", "Alice",
                                "email", "alice@example.com",
                                "date", now.toString()
                        )
                )
        );

        when(commitEntryRepository.existsByRepoIdAndSha(repoId, "abc123def456")).thenReturn(false);
        when(commitEntryRepository.save(any(CommitEntry.class))).thenReturn(new CommitEntry());

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<Map<String, Object>>>() {})))
                .thenReturn(new ResponseEntity<>(List.of(mockCommit), HttpStatus.OK));

        int imported = remoteSyncService.syncFromGitHub(repo);

        assertEquals(1, imported);
    }
}
