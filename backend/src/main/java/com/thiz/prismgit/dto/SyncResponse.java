package com.thiz.prismgit.dto;

public record SyncResponse(
        int commitsImported,
        String message
) {
}
