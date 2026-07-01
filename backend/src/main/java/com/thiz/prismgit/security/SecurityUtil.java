package com.thiz.prismgit.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtil {

    private SecurityUtil() {}

    public static UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        return null;
    }

    public static UUID requireCurrentUserId() {
        var userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return userId;
    }
}
