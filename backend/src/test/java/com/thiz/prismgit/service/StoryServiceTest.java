package com.thiz.prismgit.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.thiz.prismgit.security.SecurityUtil;
import com.thiz.prismgit.service.generator.LlmStoryGenerator;
import com.thiz.prismgit.service.generator.TemplateStoryGenerator;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private MockedStatic<SecurityUtil> securityUtilMock;

    private UUID repoId;
    private UUID storyId;
    private UUID ownerId;
    private GitRepo repo;
    private Story story;
    private CommitEntry commit1;
    private CommitEntry commit2;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::requireCurrentUserId).thenReturn(ownerId);

        repoId = UUID.randomUUID();
        storyId = UUID.randomUUID();
        repo = new GitRepo();
        repo.setId(repoId);
        repo.setName("test-repo");

        story = new Story();
        story.setId(storyId);
        story.setRepo(repo);
        story.setTitle("Test Story");
        story.setContent("# Generated");
        story.setMode(StoryMode.TEMPLATE);
        story.setCreatedAt(LocalDateTime.now());

        commit1 = new CommitEntry(UUID.randomUUID(), repo, "abc1234", "Alice", "alice@example.com",
                LocalDateTime.of(2024, 1, 1, 10, 0), "feat: add feature", "[\"src/main.ts\"]", 5, 1);
        commit2 = new CommitEntry(UUID.randomUUID(), repo, "def4567", "Bob", "bob@example.com",
                LocalDateTime.of(2024, 1, 2, 14, 0), "fix: bug fix", "[\"src/bug.ts\"]", 2, 2);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    @Test
    void should_create_story_successfully() throws JsonProcessingException {
        CreateStoryRequest request = new CreateStoryRequest("Template", null, null, null);
        story.setTitle("test-repo");
        when(repoService.findOwnedRepo(repoId)).thenReturn(repo);
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
        verify(repoService).findOwnedRepo(repoId);
        verify(commitEntryRepository).findByRepoIdOrderByAuthoredAtAsc(repoId);
        verify(templateGenerator).generate(any(), any());
        verify(storyRepository).save(any(Story.class));
    }

    @Test
    void should_throw_error_when_no_commits_in_range() {
        CreateStoryRequest request = new CreateStoryRequest("Template", null, "abc123", "xyz789");
        when(repoService.findOwnedRepo(repoId)).thenReturn(repo);
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> storyService.createStory(repoId, request));
        verify(repoService).findOwnedRepo(repoId);
    }

    @Test
    void should_parse_mode_template() throws JsonProcessingException {
        CreateStoryRequest request = new CreateStoryRequest("TEMPLATE", null, null, null);
        when(repoService.findOwnedRepo(repoId)).thenReturn(repo);
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
        lenient().when(repoService.findOwnedRepo(repoId)).thenReturn(repo);
        lenient().when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId))
                .thenReturn(List.of(commit1));

        assertThrows(IllegalArgumentException.class, () -> storyService.createStory(repoId, request));
    }

    @Test
    void should_get_story_by_id() {
        when(storyRepository.findByIdAndOwnerId(storyId, ownerId)).thenReturn(Optional.of(story));

        StoryResponse response = storyService.getStory(storyId);

        assertNotNull(response);
        assertEquals(storyId, response.id());
        assertEquals("Test Story", response.title());
        verify(storyRepository).findByIdAndOwnerId(storyId, ownerId);
    }

    @Test
    void should_throw_not_found_when_story_not_exist() {
        when(storyRepository.findByIdAndOwnerId(storyId, ownerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> storyService.getStory(storyId));
        verify(storyRepository).findByIdAndOwnerId(storyId, ownerId);
    }

    @Test
    void should_list_stories_by_repo() {
        Story story2 = new Story();
        story2.setId(UUID.randomUUID());
        story2.setRepo(repo);
        story2.setTitle("Another Story");

        when(repoService.findOwnedRepo(repoId)).thenReturn(repo);
        when(storyRepository.findByRepoIdOrderByCreatedAtDesc(repoId))
                .thenReturn(List.of(story, story2));

        List<StoryResponse> responses = storyService.listStories(repoId);

        assertEquals(2, responses.size());
        assertEquals("Test Story", responses.get(0).title());
        assertEquals("Another Story", responses.get(1).title());
        verify(repoService).findOwnedRepo(repoId);
        verify(storyRepository).findByRepoIdOrderByCreatedAtDesc(repoId);
    }

    @Test
    void should_return_empty_list_when_no_stories_in_repo() {
        when(repoService.findOwnedRepo(repoId)).thenReturn(repo);
        when(storyRepository.findByRepoIdOrderByCreatedAtDesc(repoId))
                .thenReturn(List.of());

        List<StoryResponse> responses = storyService.listStories(repoId);

        assertEquals(0, responses.size());
        verify(repoService).findOwnedRepo(repoId);
        verify(storyRepository).findByRepoIdOrderByCreatedAtDesc(repoId);
    }

    @Test
    void should_list_all_stories() {
        Story story2 = new Story();
        story2.setId(UUID.randomUUID());
        story2.setRepo(repo);
        story2.setTitle("Story 2");

        when(storyRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(List.of(story, story2));

        List<StoryResponse> responses = storyService.listAllStories();

        assertEquals(2, responses.size());
        verify(storyRepository).findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Test
    void should_return_empty_list_when_no_stories_exist() {
        when(storyRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(List.of());

        List<StoryResponse> responses = storyService.listAllStories();

        assertEquals(0, responses.size());
        verify(storyRepository).findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Test
    void should_handle_json_processing_error() throws JsonProcessingException {
        CreateStoryRequest request = new CreateStoryRequest("Template", null, null, null);
        when(repoService.findOwnedRepo(repoId)).thenReturn(repo);
        when(commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId))
                .thenReturn(List.of(commit1));
        when(templateGenerator.generate(any(), any())).thenReturn("Generated");
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonParseException(null, "JSON error"));
        when(storyRepository.save(any(Story.class))).thenReturn(story);

        StoryResponse response = storyService.createStory(repoId, request);

        assertNotNull(response);
        verify(storyRepository).save(any(Story.class));
    }
}
