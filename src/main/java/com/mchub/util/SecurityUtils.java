package com.mchub.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal.toString())) {
            throw new IllegalStateException("Could not determine current user");
        }
        return principal.toString();
    }

    public static String safeMessage(Exception e) {
        return (Objects.requireNonNull(e).getMessage() != null && !e.getMessage().isBlank())
            ? e.getMessage()
            : e.getClass().getSimpleName();
    }
}
