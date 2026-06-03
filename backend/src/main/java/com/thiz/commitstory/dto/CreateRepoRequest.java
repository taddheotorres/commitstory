package com.thiz.commitstory.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRepoRequest(
        @NotBlank String name,
        String localPath,
        String remoteUrl,
        String provider
) {
}
