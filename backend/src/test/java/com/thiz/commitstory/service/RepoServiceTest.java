package com.thiz.commitstory.service;

import com.thiz.commitstory.dto.CreateRepoRequest;
import com.thiz.commitstory.dto.RepoResponse;
import com.thiz.commitstory.dto.SyncResponse;
import com.thiz.commitstory.entity.GitRepo;
import com.thiz.commitstory.entity.RepoProvider;
import com.thiz.commitstory.exception.ResourceNotFoundException;
import com.thiz.commitstory.repository.GitRepoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepoServiceTest {

    @Mock
    private GitRepoRepository gitRepoRepository;

    @Mock
    private RepoSyncService repoSyncService;

    @InjectMocks
    private RepoService repoService;

    private UUID repoId;
    private GitRepo repo;
    private CreateRepoRequest createRequest;

    @BeforeEach
    void setUp() {
        repoId = UUID.randomUUID();
        repo = new GitRepo();
        repo.setId(repoId);
        repo.setName("test-repo");
        repo.setLocalPath("/local/path");
        repo.setRemoteUrl("https://github.com/user/repo");
        repo.setProvider(RepoProvider.GITHUB);
        repo.setCreatedAt(LocalDateTime.now());

        createRequest = new CreateRepoRequest("test-repo", "/local/path", "https://github.com/user/repo", "github");
    }

    @Test
    void should_create_repo_successfully() {
        when(gitRepoRepository.save(any(GitRepo.class))).thenReturn(repo);

        RepoResponse response = repoService.createRepo(createRequest);

        assertNotNull(response);
        assertEquals("test-repo", response.name());
        assertEquals("/local/path", response.localPath());
        assertEquals(RepoProvider.GITHUB, response.provider());
        verify(gitRepoRepository).save(any(GitRepo.class));
    }

    @Test
    void should_create_repo_with_default_provider() {
        CreateRepoRequest request = new CreateRepoRequest("test-repo", "/path", null, null);
        GitRepo repoWithDefault = new GitRepo();
        repoWithDefault.setId(repoId);
        repoWithDefault.setName("test-repo");
        repoWithDefault.setLocalPath("/path");
        repoWithDefault.setProvider(RepoProvider.NONE);
        repoWithDefault.setCreatedAt(LocalDateTime.now());

        when(gitRepoRepository.save(any(GitRepo.class))).thenReturn(repoWithDefault);

        RepoResponse response = repoService.createRepo(request);

        assertEquals(RepoProvider.NONE, response.provider());
        verify(gitRepoRepository).save(any(GitRepo.class));
    }

    @Test
    void should_get_repo_by_id() {
        when(gitRepoRepository.findById(repoId)).thenReturn(Optional.of(repo));

        RepoResponse response = repoService.getRepo(repoId);

        assertNotNull(response);
        assertEquals(repoId, response.id());
        assertEquals("test-repo", response.name());
        verify(gitRepoRepository).findById(repoId);
    }

    @Test
    void should_throw_not_found_when_get_nonexistent_repo() {
        when(gitRepoRepository.findById(repoId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repoService.getRepo(repoId));
        verify(gitRepoRepository).findById(repoId);
    }

    @Test
    void should_list_all_repos() {
        GitRepo repo2 = new GitRepo();
        repo2.setId(UUID.randomUUID());
        repo2.setName("another-repo");

        when(gitRepoRepository.findAll()).thenReturn(List.of(repo, repo2));

        List<RepoResponse> responses = repoService.listRepos();

        assertEquals(2, responses.size());
        assertEquals("test-repo", responses.get(0).name());
        assertEquals("another-repo", responses.get(1).name());
        verify(gitRepoRepository).findAll();
    }

    @Test
    void should_return_empty_list_when_no_repos() {
        when(gitRepoRepository.findAll()).thenReturn(List.of());

        List<RepoResponse> responses = repoService.listRepos();

        assertEquals(0, responses.size());
        verify(gitRepoRepository).findAll();
    }

    @Test
    void should_delete_repo_successfully() {
        when(gitRepoRepository.findById(repoId)).thenReturn(Optional.of(repo));

        repoService.deleteRepo(repoId);

        verify(gitRepoRepository).findById(repoId);
        verify(gitRepoRepository).delete(repo);
    }

    @Test
    void should_throw_not_found_when_delete_nonexistent_repo() {
        when(gitRepoRepository.findById(repoId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repoService.deleteRepo(repoId));
        verify(gitRepoRepository).findById(repoId);
    }

    @Test
    void should_sync_repo_successfully() {
        SyncResponse syncResponse = new SyncResponse(5, "Synced 5 commits");
        when(gitRepoRepository.findById(repoId)).thenReturn(Optional.of(repo));
        when(repoSyncService.sync(repo)).thenReturn(syncResponse);

        SyncResponse response = repoService.syncRepo(repoId);

        assertNotNull(response);
        assertEquals(5, response.commitsAdded());
        assertEquals("Synced 5 commits", response.message());
        verify(gitRepoRepository).findById(repoId);
        verify(repoSyncService).sync(repo);
    }

    @Test
    void should_throw_not_found_when_sync_nonexistent_repo() {
        when(gitRepoRepository.findById(repoId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repoService.syncRepo(repoId));
        verify(gitRepoRepository).findById(repoId);
    }

    @Test
    void should_find_repo_by_id() {
        when(gitRepoRepository.findById(repoId)).thenReturn(Optional.of(repo));

        GitRepo found = repoService.findRepo(repoId);

        assertNotNull(found);
        assertEquals(repoId, found.getId());
        assertEquals("test-repo", found.getName());
        verify(gitRepoRepository).findById(repoId);
    }

    @Test
    void should_throw_not_found_when_finding_nonexistent_repo() {
        when(gitRepoRepository.findById(repoId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repoService.findRepo(repoId));
    }
}
