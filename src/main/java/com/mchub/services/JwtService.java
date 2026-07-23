package com.mchub.services;

import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.function.Function;

public interface JwtService {

    String extractUserId(String token);

    String extractRole(String token);

    Date extractIssuedAt(String token);

    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    String generateToken(String userId, String role);

    String generateToken(String userId);

    boolean isTokenValid(String token, String userId);

    /**
     * Short-lived (10 min) token carrying an unverified-account Google identity, used only
     * between POST /auth/google (new email) and POST /auth/google/complete-registration.
     * Distinct from a normal auth token: JwtAuthenticationFilter rejects it outright because
     * it carries no "id" claim, only "pendingGoogleId"/"pendingGoogleEmail".
     */
    String generatePendingGoogleToken(String googleId, String email, String name);

    PendingGoogleIdentity extractPendingGoogleIdentity(String pendingToken);

    record PendingGoogleIdentity(String googleId, String email, String name) {
    }
}
