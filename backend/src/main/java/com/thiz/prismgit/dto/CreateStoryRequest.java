package com.thiz.prismgit.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateStoryRequest(
        @NotBlank String mode,
        String title,
        String startSha,
        String endSha
) {
}
