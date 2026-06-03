package com.thiz.commitstory.dto;

public record SyncResponse(
        int commitsImported,
        String message
) {
}
