package com.thiz.commitstory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiz.commitstory.dto.CreateRepoRequest;
import com.thiz.commitstory.dto.RepoResponse;
import com.thiz.commitstory.dto.SyncResponse;
import com.thiz.commitstory.entity.CommitEntry;
import com.thiz.commitstory.entity.RepoProvider;
import com.thiz.commitstory.exception.ResourceNotFoundException;
import com.thiz.commitstory.repository.CommitEntryRepository;
import com.thiz.commitstory.service.RepoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RepoController.class)
class RepoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RepoService repoService;

    @MockitoBean
    private CommitEntryRepository commitEntryRepository;

    private UUID repoId;
    private CreateRepoRequest createRequest;
    private RepoResponse repoResponse;

    @BeforeEach
    void setUp() {
        repoId = UUID.randomUUID();
        createRequest = new CreateRepoRequest("test-repo", "/local/path", "https://github.com/user/repo", "github");
        repoResponse = new RepoResponse(
                repoId, "test-repo", "/local/path", "https://github.com/user/repo",
                RepoProvider.GITHUB, LocalDateTime.now()
        );
    }

    @Test
    void should_create_repo_successfully() throws Exception {
        when(repoService.createRepo(any(CreateRepoRequest.class)))
                .thenReturn(repoResponse);

        mockMvc.perform(post("/api/repos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(repoId.toString()))
                .andExpect(jsonPath("$.name").value("test-repo"))
                .andExpect(jsonPath("$.localPath").value("/local/path"));

        verify(repoService).createRepo(any(CreateRepoRequest.class));
    }

    @Test
    void should_return_400_when_create_repo_with_invalid_data() throws Exception {
        CreateRepoRequest invalidRequest = new CreateRepoRequest("", "", "", "");

        mockMvc.perform(post("/api/repos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_list_repos_successfully() throws Exception {
        RepoResponse repo2 = new RepoResponse(
                UUID.randomUUID(), "another-repo", "/another/path", null,
                RepoProvider.NONE, LocalDateTime.now()
        );
        when(repoService.listRepos()).thenReturn(List.of(repoResponse, repo2));

        mockMvc.perform(get("/api/repos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(repoId.toString()))
                .andExpect(jsonPath("$[0].name").value("test-repo"))
                .andExpect(jsonPath("$[1].name").value("another-repo"))
                .andExpect(jsonPath("$.length()").value(2));

        verify(repoService).listRepos();
    }

    @Test
    void should_return_empty_list_when_no_repos() throws Exception {
        when(repoService.listRepos()).thenReturn(List.of());

        mockMvc.perform(get("/api/repos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(repoService).listRepos();
    }

    @Test
    void should_get_repo_by_id_successfully() throws Exception {
        when(repoService.getRepo(repoId)).thenReturn(repoResponse);

        mockMvc.perform(get("/api/repos/{id}", repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(repoId.toString()))
                .andExpect(jsonPath("$.name").value("test-repo"));

        verify(repoService).getRepo(repoId);
    }

    @Test
    void should_return_404_when_get_nonexistent_repo() throws Exception {
        when(repoService.getRepo(repoId))
                .thenThrow(new ResourceNotFoundException("GitRepo", repoId));

        mockMvc.perform(get("/api/repos/{id}", repoId))
                .andExpect(status().isNotFound());

        verify(repoService).getRepo(repoId);
    }

    @Test
    void should_delete_repo_successfully() throws Exception {
        mockMvc.perform(delete("/api/repos/{id}", repoId))
                .andExpect(status().isNoContent());

        verify(repoService).deleteRepo(repoId);
    }

    @Test
    void should_return_404_when_delete_nonexistent_repo() throws Exception {
        doThrow(new ResourceNotFoundException("GitRepo", repoId))
                .when(repoService).deleteRepo(repoId);

        mockMvc.perform(delete("/api/repos/{id}", repoId))
                .andExpect(status().isNotFound());

        verify(repoService).deleteRepo(repoId);
    }

    @Test
    void should_sync_repo_successfully() throws Exception {
        SyncResponse syncResponse = new SyncResponse(5, "Synced 5 commits");
        when(repoService.syncRepo(repoId)).thenReturn(syncResponse);

        mockMvc.perform(post("/api/repos/{id}/sync", repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commitsAdded").value(5))
                .andExpect(jsonPath("$.message").value("Synced 5 commits"));

        verify(repoService).syncRepo(repoId);
    }

    @Test
    void should_list_commits_with_pagination() throws Exception {
        CommitEntry commit = new CommitEntry(
                UUID.randomUUID(), null, "abc123", "Alice", "alice@example.com",
                LocalDateTime.now(), "feat: add feature", "[\"src/main.ts\"]", 10, 2
        );
        var page = new PageImpl<>(List.of(commit), PageRequest.of(0, 50), 1);
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtDesc(eq(repoId), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/repos/{id}/commits", repoId)
                .param("page", "0")
                .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sha").value("abc123"))
                .andExpect(jsonPath("$[0].authorName").value("Alice"))
                .andExpect(jsonPath("$.length()").value(1));

        verify(commitEntryRepository).findByRepoIdOrderByAuthoredAtDesc(eq(repoId), any(PageRequest.class));
    }

    @Test
    void should_list_commits_with_default_pagination() throws Exception {
        var page = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtDesc(eq(repoId), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/repos/{id}/commits", repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(commitEntryRepository).findByRepoIdOrderByAuthoredAtDesc(eq(repoId), any(PageRequest.class));
    }
}
