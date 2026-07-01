package com.thiz.prismgit.service;

import com.thiz.prismgit.dto.CreateStoryRequest;
import com.thiz.prismgit.dto.StoryResponse;
import com.thiz.prismgit.entity.CommitEntry;
import com.thiz.prismgit.entity.Story;
import com.thiz.prismgit.entity.StoryMode;
import com.thiz.prismgit.exception.ResourceNotFoundException;
import com.thiz.prismgit.repository.CommitEntryRepository;
import com.thiz.prismgit.repository.StoryRepository;
import com.thiz.prismgit.security.SecurityUtil;
import com.thiz.prismgit.service.generator.LlmStoryGenerator;
import com.thiz.prismgit.service.generator.StoryGenerator;
import com.thiz.prismgit.service.generator.TemplateStoryGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final CommitEntryRepository commitEntryRepository;
    private final RepoService repoService;
    private final TemplateStoryGenerator templateGenerator;
    private final LlmStoryGenerator llmGenerator;
    private final ObjectMapper objectMapper;

    @Transactional
    public StoryResponse createStory(UUID repoId, CreateStoryRequest request) {
        log.info("Creating story for repository: {} (mode: {})", repoId, request.mode());
        var repo = repoService.findOwnedRepo(repoId);
        var mode = parseMode(request.mode());

        var commits = fetchCommitsInRange(repoId, request.startSha(), request.endSha());
        if (commits.isEmpty()) {
            log.warn("No commits found for range in repository: {}", repoId);
            throw new IllegalArgumentException("No commits found for the given range");
        }

        log.debug("Generating story with {} commits in {} mode", commits.size(), mode);
        var generator = resolveGenerator(mode);
        var options = new LinkedHashMap<String, String>();
        options.put("title", request.title() != null ? request.title() : repo.getName());

        var content = generator.generate(commits, options);

        var metadata = Map.of(
                "authorCount", commits.stream().map(CommitEntry::getAuthorEmail).distinct().count(),
                "commitCount", commits.size()
        );

        var story = new Story();
        story.setRepo(repo);
        story.setTitle(request.title() != null ? request.title() : repo.getName());
        story.setContent(content);
        story.setMode(mode);
        story.setStartSha(request.startSha());
        story.setEndSha(request.endSha());
        story.setOwnerId(SecurityUtil.requireCurrentUserId());
        try {
            story.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            log.error("Error serializing story metadata", e);
            story.setMetadata("{}");
        }
        story = storyRepository.save(story);
        log.info("Story created successfully: {} (ID: {})", story.getTitle(), story.getId());

        return toResponse(story);
    }

    public StoryResponse getStory(UUID id) {
        log.debug("Fetching story: {}", id);
        var ownerId = SecurityUtil.requireCurrentUserId();
        var story = storyRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> {
                    log.warn("Story not found: {}", id);
                    return new ResourceNotFoundException("Story", id);
                });
        return toResponse(story);
    }

    public List<StoryResponse> listStories(UUID repoId) {
        log.debug("Listing stories for repository: {}", repoId);
        repoService.findOwnedRepo(repoId);
        return storyRepository.findByRepoIdOrderByCreatedAtDesc(repoId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<StoryResponse> listAllStories() {
        log.debug("Listing all stories");
        var ownerId = SecurityUtil.requireCurrentUserId();
        return storyRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private List<CommitEntry> fetchCommitsInRange(UUID repoId, String startSha, String endSha) {
        var allCommits = commitEntryRepository.findByRepoIdOrderByAuthoredAtAsc(repoId);
        if (startSha == null && endSha == null) {
            return allCommits;
        }

        int startIdx = 0;
        int endIdx = allCommits.size() - 1;

        for (int i = 0; i < allCommits.size(); i++) {
            if (allCommits.get(i).getSha().equals(startSha)) {
                startIdx = i;
            }
            if (allCommits.get(i).getSha().equals(endSha)) {
                endIdx = i;
            }
        }

        return allCommits.subList(startIdx, endIdx + 1);
    }

    private StoryGenerator resolveGenerator(StoryMode mode) {
        return switch (mode) {
            case TEMPLATE -> templateGenerator;
            case LLM -> llmGenerator;
        };
    }

    private StoryMode parseMode(String mode) {
        try {
            return StoryMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid story mode: {}", mode);
            throw new IllegalArgumentException("Invalid mode: " + mode + ". Valid values: TEMPLATE, LLM");
        }
    }

    private StoryResponse toResponse(Story story) {
        return new StoryResponse(
                story.getId(),
                story.getRepo().getId(),
                story.getTitle(),
                story.getContent(),
                story.getMode(),
                story.getStartSha(),
                story.getEndSha(),
                story.getCreatedAt()
        );
    }
}
