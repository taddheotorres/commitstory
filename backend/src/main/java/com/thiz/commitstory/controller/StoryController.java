package com.thiz.commitstory.controller;

import com.thiz.commitstory.dto.CreateStoryRequest;
import com.thiz.commitstory.dto.StoryResponse;
import com.thiz.commitstory.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Stories", description = "Generate and manage narrative stories from commits")
public class StoryController {

    private final StoryService storyService;

    @PostMapping("/repos/{repoId}/stories")
    @Operation(summary = "Create a story", description = "Generate a narrative story from repository commits")
    @ApiResponse(responseCode = "201", description = "Story created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or no commits in range")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<StoryResponse> createStory(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID repoId,
            @Valid @RequestBody CreateStoryRequest request) {
        var response = storyService.createStory(repoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/stories")
    @Operation(summary = "List stories", description = "Get all stories or filter by repository")
    @ApiResponse(responseCode = "200", description = "List of stories")
    public ResponseEntity<List<StoryResponse>> listStories(
            @Parameter(description = "Filter by repository ID (optional)")
            @RequestParam(required = false) UUID repoId) {
        if (repoId != null) {
            return ResponseEntity.ok(storyService.listStories(repoId));
        }
        return ResponseEntity.ok(storyService.listAllStories());
    }

    @GetMapping("/stories/{id}")
    @Operation(summary = "Get story by ID", description = "Retrieve a specific story")
    @ApiResponse(responseCode = "200", description = "Story found")
    @ApiResponse(responseCode = "404", description = "Story not found")
    public ResponseEntity<StoryResponse> getStory(
            @Parameter(description = "Story ID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(storyService.getStory(id));
    }

    @GetMapping("/repos/{repoId}/stories")
    @Operation(summary = "List stories by repository", description = "Get all stories for a specific repository")
    @ApiResponse(responseCode = "200", description = "List of stories")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    public ResponseEntity<List<StoryResponse>> listStoriesByRepo(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID repoId) {
        return ResponseEntity.ok(storyService.listStories(repoId));
    }
}
