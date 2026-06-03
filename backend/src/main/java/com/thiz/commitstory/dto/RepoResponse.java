package com.thiz.commitstory.dto;

import com.thiz.commitstory.entity.RepoProvider;

import java.time.LocalDateTime;
import java.util.UUID;

public record RepoResponse(
        UUID id,
        String name,
        String localPath,
        String remoteUrl,
        RepoProvider provider,
        LocalDateTime createdAt
) {
}
