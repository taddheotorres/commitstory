package com.thiz.prismgit.dto;

import com.thiz.prismgit.entity.StoryMode;

import java.time.LocalDateTime;
import java.util.UUID;

public record StoryResponse(
        UUID id,
        UUID repoId,
        String title,
        String content,
        StoryMode mode,
        String startSha,
        String endSha,
        LocalDateTime createdAt
) {
}
