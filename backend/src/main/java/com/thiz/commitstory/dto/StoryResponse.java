package com.thiz.commitstory.dto;

import com.thiz.commitstory.entity.StoryMode;

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
