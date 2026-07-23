package com.mchub.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Single source of truth for extracting a client IP used in rate-limiting / abuse-prevention
 * decisions (login throttling, OTP throttling, guest voice-analysis cooldown).
 *
 * Trusts X-Forwarded-For because production only runs behind Render's proxy, which sets this
 * header itself and is not reachable directly by clients. If this app is ever exposed without
 * a trusted reverse proxy in front of it, this header becomes attacker-controlled and every
 * caller of this method must be re-audited.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
