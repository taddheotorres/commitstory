package com.thiz.prismgit.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommitResponse(
        UUID id,
        String sha,
        String authorName,
        String authorEmail,
        LocalDateTime authoredAt,
        String message,
        List<String> filesChanged,
        int additions,
        int deletions
) {
}
