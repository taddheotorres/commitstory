package com.thiz.prismgit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiz.prismgit.dto.CreateStoryRequest;
import com.thiz.prismgit.dto.StoryResponse;
import com.thiz.prismgit.entity.CommitEntry;
import com.thiz.prismgit.entity.GitRepo;
import com.thiz.prismgit.entity.Story;
import com.thiz.prismgit.entity.StoryMode;
import com.thiz.prismgit.exception.ResourceNotFoundException;
import com.thiz.prismgit.repository.CommitEntryRepository;
import com.thiz.prismgit.repository.StoryRepository;
import com.thiz.prismgit.service.generator.LlmStoryGenerator;
import com.thiz.prismgit.service.generator.TemplateStoryGenerator;
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
class StoryServiceTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private CommitEntryRepository commitEntryRepository;

    @Mock
    private RepoService repoService;

    @Mock
    private TemplateStoryGenerator templateGenerator;

    @Mock
    private LlmStoryGenerator llmGenerator;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private StoryService storyService;

    private UUID repoId;
    private UUID storyId;
    private GitRepo repo;
    private Story story;
    private CommitEntry commit1, commit2;

    @BeforeEach
    void setUp() {
        repoId = UUID.randomUUID();
        storyId = UUID.randomUUID();

        repo = new GitRepo();
        repo.setId(repoId);
        repo.setName("test-repo");

        story = new Story();
        story.setId(storyId);
        story.setRepo(repo);
        story.setTitle("Test Story");
        story.setContent("# Story Content");
        story.setMode(StoryMode.TEMPLATE);
        story.setCreatedAt(LocalDateTime.now());

        commit1 = new CommitEntry(UUID.randomUUID(), repo, "abc123", "Alice", "alice@example.com",
                LocalDateTime.of(2024, 1, 1, 10, 0), "feat: add feature", "[\"src/main.ts\"]", 5, 1);
        commit2 = new CommitEntry(UUID.randomUUID(), repo, "def456", "Bob", "bob@example.com",
                LocalDateTime.of(2024, 1, 2, 14, 0), "fix: bug fix", "[\"src/bug.ts\"]", 2, 2);
    }

    @Test
    void should_create_story_successfully() {
        CreateStoryRequest request = new CreateStoryRequest("Template", null, null, null);
        when(repoService.findRepo(repoId)).thenReturn(repo);
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId))
                .thenReturn(List.of(commit1, commit2));
        when(templateGenerator.generate(any(), any())).thenReturn("# Generated Story");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"authorCount\":2,\"commitCount\":2}");
        when(storyRepository.save(any(Story.class))).thenReturn(story);

        StoryResponse response = storyService.createStory(repoId, request);

        assertNotNull(response);
        assertEquals(storyId, response.id());
        assertEquals(repoId, response.repoId());
        assertEquals("test-repo", response.title());
        verify(repoService).findRepo(repoId);
        verify(commitEntryRepository).findByRepoIdOrderByAuthoredAtAsc(repoId);
        verify(templateGenerator).generate(any(), any());
        verify(storyRepository).save(any(Story.class));
    }

    @Test
    void should_throw_error_when_no_commits_in_range() {
        CreateStoryRequest request = new CreateStoryRequest("Template", null, "abc123", "xyz789");
        when(repoService.findRepo(repoId)).thenReturn(repo);
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> storyService.createStory(repoId, request));
        verify(repoService).findRepo(repoId);
    }

    @Test
    void should_parse_mode_template() {
        CreateStoryRequest request = new CreateStoryRequest("TEMPLATE", null, null, null);
        when(repoService.findRepo(repoId)).thenReturn(repo);
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId))
                .thenReturn(List.of(commit1));
        when(templateGenerator.generate(any(), any())).thenReturn("Generated");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(storyRepository.save(any(Story.class))).thenReturn(story);

        StoryResponse response = storyService.createStory(repoId, request);

        assertNotNull(response);
        assertEquals(StoryMode.TEMPLATE, response.mode());
    }

    @Test
    void should_throw_error_on_invalid_mode() {
        CreateStoryRequest request = new CreateStoryRequest("INVALID", null, null, null);
        when(repoService.findRepo(repoId)).thenReturn(repo);
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId))
                .thenReturn(List.of(commit1));

        assertThrows(IllegalArgumentException.class, () -> storyService.createStory(repoId, request));
    }

    @Test
    void should_get_story_by_id() {
        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));

        StoryResponse response = storyService.getStory(storyId);

        assertNotNull(response);
        assertEquals(storyId, response.id());
        assertEquals("Test Story", response.title());
        verify(storyRepository).findById(storyId);
    }

    @Test
    void should_throw_not_found_when_story_not_exist() {
        when(storyRepository.findById(storyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> storyService.getStory(storyId));
        verify(storyRepository).findById(storyId);
    }

    @Test
    void should_list_stories_by_repo() {
        Story story2 = new Story();
        story2.setId(UUID.randomUUID());
        story2.setRepo(repo);
        story2.setTitle("Another Story");

        when(storyRepository.findByRepoIdOrderByCreatedAtDesc(repoId))
                .thenReturn(List.of(story, story2));

        List<StoryResponse> responses = storyService.listStories(repoId);

        assertEquals(2, responses.size());
        assertEquals("Test Story", responses.get(0).title());
        assertEquals("Another Story", responses.get(1).title());
        verify(storyRepository).findByRepoIdOrderByCreatedAtDesc(repoId);
    }

    @Test
    void should_return_empty_list_when_no_stories_in_repo() {
        when(storyRepository.findByRepoIdOrderByCreatedAtDesc(repoId))
                .thenReturn(List.of());

        List<StoryResponse> responses = storyService.listStories(repoId);

        assertEquals(0, responses.size());
        verify(storyRepository).findByRepoIdOrderByCreatedAtDesc(repoId);
    }

    @Test
    void should_list_all_stories() {
        UUID otherRepoId = UUID.randomUUID();
        GitRepo otherRepo = new GitRepo();
        otherRepo.setId(otherRepoId);

        Story story2 = new Story();
        story2.setId(UUID.randomUUID());
        story2.setRepo(otherRepo);
        story2.setTitle("Story from Other Repo");

        when(storyRepository.findAll()).thenReturn(List.of(story, story2));

        List<StoryResponse> responses = storyService.listAllStories();

        assertEquals(2, responses.size());
        verify(storyRepository).findAll();
    }

    @Test
    void should_return_empty_list_when_no_stories_exist() {
        when(storyRepository.findAll()).thenReturn(List.of());

        List<StoryResponse> responses = storyService.listAllStories();

        assertEquals(0, responses.size());
        verify(storyRepository).findAll();
    }

    @Test
    void should_handle_json_processing_error() {
        CreateStoryRequest request = new CreateStoryRequest("Template", null, null, null);
        when(repoService.findRepo(repoId)).thenReturn(repo);
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId))
                .thenReturn(List.of(commit1));
        when(templateGenerator.generate(any(), any())).thenReturn("Generated");
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("JSON error"));
        when(storyRepository.save(any(Story.class))).thenReturn(story);

        StoryResponse response = storyService.createStory(repoId, request);

        assertNotNull(response);
        verify(storyRepository).save(any(Story.class));
    }
}
