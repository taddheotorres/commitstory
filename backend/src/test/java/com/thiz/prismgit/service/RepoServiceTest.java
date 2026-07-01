package com.thiz.prismgit.service;

import com.thiz.prismgit.dto.CreateRepoRequest;
import com.thiz.prismgit.dto.RepoResponse;
import com.thiz.prismgit.dto.SyncResponse;
import com.thiz.prismgit.entity.GitRepo;
import com.thiz.prismgit.entity.RepoProvider;
import com.thiz.prismgit.exception.ResourceNotFoundException;
import com.thiz.prismgit.repository.GitRepoRepository;
import com.thiz.prismgit.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepoServiceTest {

    @Mock
    private GitRepoRepository gitRepoRepository;

    @Mock
    private RepoSyncService repoSyncService;

    @InjectMocks
    private RepoService repoService;

    private MockedStatic<SecurityUtil> securityUtilMock;

    private UUID repoId;
    private UUID ownerId;
    private GitRepo repo;
    private CreateRepoRequest createRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        repoId = UUID.randomUUID();
        repo = new GitRepo();
        repo.setId(repoId);
        repo.setName("test-repo");
        repo.setLocalPath("/local/path");
        repo.setRemoteUrl("https://github.com/user/repo");
        repo.setProvider(RepoProvider.GITHUB);
        repo.setOwnerId(ownerId);
        repo.setCreatedAt(LocalDateTime.now());

        createRequest = new CreateRepoRequest("test-repo", "/local/path", "https://github.com/user/repo", "github");
    }

    @AfterEach
    void tearDown() {
        if (securityUtilMock != null) {
            securityUtilMock.close();
        }
    }

    @Test
    void should_create_repo_successfully() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::requireCurrentUserId).thenReturn(ownerId);
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
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::requireCurrentUserId).thenReturn(ownerId);

        CreateRepoRequest request = new CreateRepoRequest("test-repo", "/path", null, null);
        GitRepo repoWithDefault = new GitRepo();
        repoWithDefault.setId(repoId);
        repoWithDefault.setName("test-repo");
        repoWithDefault.setLocalPath("/path");
        repoWithDefault.setProvider(RepoProvider.NONE);
        repoWithDefault.setOwnerId(ownerId);
        repoWithDefault.setCreatedAt(LocalDateTime.now());

        when(gitRepoRepository.save(any(GitRepo.class))).thenReturn(repoWithDefault);

        RepoResponse response = repoService.createRepo(request);

        assertEquals(RepoProvider.NONE, response.provider());
        verify(gitRepoRepository).save(any(GitRepo.class));
    }

    @Test
    void should_get_repo_by_id() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::requireCurrentUserId).thenReturn(ownerId);
        when(gitRepoRepository.findByIdAndOwnerId(repoId, ownerId)).thenReturn(Optional.of(repo));

        RepoResponse response = repoService.getRepo(repoId);

        assertNotNull(response);
        assertEquals(repoId, response.id());
        assertEquals("test-repo", response.name());
        verify(gitRepoRepository).findByIdAndOwnerId(repoId, ownerId);
    }

    @Test
    void should_throw_not_found_when_get_nonexistent_repo() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::requireCurrentUserId).thenReturn(ownerId);
        when(gitRepoRepository.findByIdAndOwnerId(repoId, ownerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repoService.getRepo(repoId));
        verify(gitRepoRepository).findByIdAndOwnerId(repoId, ownerId);
    }

    @Test
    void should_list_all_repos() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::requireCurrentUserId).thenReturn(ownerId);

        GitRepo repo2 = new GitRepo();
        repo2.setId(UUID.randomUUID());
        repo2.setName("another-repo");
        repo2.setOwnerId(ownerId);

        when(gitRepoRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(List.of(repo, repo2));

        List<RepoResponse> responses = repoService.listRepos();

        assertEquals(2, responses.size());
        assertEquals("test-repo", responses.get(0).name());
        assertEquals("another-repo", responses.get(1).name());
        verify(gitRepoRepository).findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Test
    void should_return_empty_list_when_no_repos() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::requireCurrentUserId).thenReturn(ownerId);
        when(gitRepoRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(List.of());

        List<RepoResponse> responses = repoService.listRepos();

        assertEquals(0, responses.size());
        verify(gitRepoRepository).findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Test
    void should_delete_repo_successfully() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::requireCurrentUserId).thenReturn(ownerId);
        when(gitRepoRepository.findByIdAndOwnerId(repoId, ownerId)).thenReturn(Optional.of(repo));

        repoService.deleteRepo(repoId);

        verify(gitRepoRepository).findByIdAndOwnerId(repoId, ownerId);
        verify(gitRepoRepository).delete(repo);
    }

    @Test
    void should_throw_not_found_when_delete_nonexistent_repo() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::requireCurrentUserId).thenReturn(ownerId);
        when(gitRepoRepository.findByIdAndOwnerId(repoId, ownerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repoService.deleteRepo(repoId));
        verify(gitRepoRepository).findByIdAndOwnerId(repoId, ownerId);
    }

    @Test
    void should_sync_repo_successfully() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::requireCurrentUserId).thenReturn(ownerId);

        SyncResponse syncResponse = new SyncResponse(5, "Synced 5 commits");
        when(gitRepoRepository.findByIdAndOwnerId(repoId, ownerId)).thenReturn(Optional.of(repo));
        when(repoSyncService.sync(repo)).thenReturn(syncResponse);

        SyncResponse response = repoService.syncRepo(repoId);

        assertNotNull(response);
        assertEquals(5, response.commitsImported());
        assertEquals("Synced 5 commits", response.message());
        verify(gitRepoRepository).findByIdAndOwnerId(repoId, ownerId);
        verify(repoSyncService).sync(repo);
    }

    @Test
    void should_throw_not_found_when_sync_nonexistent_repo() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::requireCurrentUserId).thenReturn(ownerId);
        when(gitRepoRepository.findByIdAndOwnerId(repoId, ownerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repoService.syncRepo(repoId));
        verify(gitRepoRepository).findByIdAndOwnerId(repoId, ownerId);
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
