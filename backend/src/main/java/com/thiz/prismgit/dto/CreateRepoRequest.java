package com.thiz.prismgit.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRepoRequest(
        @NotBlank String name,
        String localPath,
        String remoteUrl,
        String provider
) {
}
