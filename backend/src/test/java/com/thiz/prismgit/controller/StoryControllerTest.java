package com.thiz.prismgit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiz.prismgit.dto.CreateStoryRequest;
import com.thiz.prismgit.dto.StoryResponse;
import com.thiz.prismgit.entity.StoryMode;
import com.thiz.prismgit.exception.ResourceNotFoundException;
import com.thiz.prismgit.security.JwtAuthenticationFilter;
import com.thiz.prismgit.service.StoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class StoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StoryService storyService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UUID repoId;
    private UUID storyId;
    private CreateStoryRequest createRequest;
    private StoryResponse storyResponse;

    @BeforeEach
    void setUp() {
        repoId = UUID.randomUUID();
        storyId = UUID.randomUUID();
        createRequest = new CreateStoryRequest("Template", null, null, null);
        storyResponse = new StoryResponse(
                storyId, repoId, "The Journey of This Repo", "## Chapter 1\nLots of commits...",
                StoryMode.TEMPLATE, null, null, LocalDateTime.now()
        );
    }

    @Test
    void should_create_story_successfully() throws Exception {
        when(storyService.createStory(eq(repoId), any(CreateStoryRequest.class)))
                .thenReturn(storyResponse);

        mockMvc.perform(post("/api/repos/{repoId}/stories", repoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(storyId.toString()))
                .andExpect(jsonPath("$.repoId").value(repoId.toString()))
                .andExpect(jsonPath("$.title").value("The Journey of This Repo"))
                .andExpect(jsonPath("$.mode").value("TEMPLATE"));

        verify(storyService).createStory(eq(repoId), any(CreateStoryRequest.class));
    }

    @Test
    void should_return_400_when_create_story_with_invalid_data() throws Exception {
        CreateStoryRequest invalidRequest = new CreateStoryRequest("", null, null, null);

        mockMvc.perform(post("/api/repos/{repoId}/stories", repoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_list_all_stories() throws Exception {
        StoryResponse story2 = new StoryResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Another Story", "Content",
                StoryMode.LLM, null, null, LocalDateTime.now()
        );
        when(storyService.listAllStories()).thenReturn(List.of(storyResponse, story2));

        mockMvc.perform(get("/api/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(storyId.toString()))
                .andExpect(jsonPath("$[0].title").value("The Journey of This Repo"))
                .andExpect(jsonPath("$[1].title").value("Another Story"))
                .andExpect(jsonPath("$.length()").value(2));

        verify(storyService).listAllStories();
    }

    @Test
    void should_list_stories_by_repo_id() throws Exception {
        when(storyService.listStories(repoId)).thenReturn(List.of(storyResponse));

        mockMvc.perform(get("/api/stories")
                .param("repoId", repoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repoId").value(repoId.toString()))
                .andExpect(jsonPath("$.length()").value(1));

        verify(storyService).listStories(repoId);
    }

    @Test
    void should_list_stories_by_repo_path() throws Exception {
        when(storyService.listStories(repoId)).thenReturn(List.of(storyResponse));

        mockMvc.perform(get("/api/repos/{repoId}/stories", repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repoId").value(repoId.toString()))
                .andExpect(jsonPath("$.length()").value(1));

        verify(storyService).listStories(repoId);
    }

    @Test
    void should_return_empty_list_when_no_stories() throws Exception {
        when(storyService.listAllStories()).thenReturn(List.of());

        mockMvc.perform(get("/api/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(storyService).listAllStories();
    }

    @Test
    void should_get_story_by_id() throws Exception {
        when(storyService.getStory(storyId)).thenReturn(storyResponse);

        mockMvc.perform(get("/api/stories/{id}", storyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storyId.toString()))
                .andExpect(jsonPath("$.title").value("The Journey of This Repo"))
                .andExpect(jsonPath("$.mode").value("TEMPLATE"));

        verify(storyService).getStory(storyId);
    }

    @Test
    void should_return_404_when_get_nonexistent_story() throws Exception {
        when(storyService.getStory(storyId))
                .thenThrow(new ResourceNotFoundException("Story", storyId));

        mockMvc.perform(get("/api/stories/{id}", storyId))
                .andExpect(status().isNotFound());

        verify(storyService).getStory(storyId);
    }
}
