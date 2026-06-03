package com.thiz.commitstory.controller;

import com.thiz.commitstory.dto.CreateStoryRequest;
import com.thiz.commitstory.dto.StoryResponse;
import com.thiz.commitstory.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @PostMapping("/repos/{repoId}/stories")
    public ResponseEntity<StoryResponse> createStory(
            @PathVariable UUID repoId,
            @Valid @RequestBody CreateStoryRequest request) {
        var response = storyService.createStory(repoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/stories")
    public ResponseEntity<List<StoryResponse>> listStories() {
        // TODO: support filtering by repoId
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/stories/{id}")
    public ResponseEntity<StoryResponse> getStory(@PathVariable UUID id) {
        return ResponseEntity.ok(storyService.getStory(id));
    }

    @GetMapping("/repos/{repoId}/stories")
    public ResponseEntity<List<StoryResponse>> listStoriesByRepo(@PathVariable UUID repoId) {
        return ResponseEntity.ok(storyService.listStories(repoId));
    }
}
