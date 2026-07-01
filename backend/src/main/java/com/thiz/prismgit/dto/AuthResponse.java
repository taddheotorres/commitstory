package com.thiz.prismgit.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        long expiresIn,
        UUID userId,
        String name,
        String email
) {
}
