package com.thiz.prismgit.dto;

import com.thiz.prismgit.entity.RepoProvider;

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
