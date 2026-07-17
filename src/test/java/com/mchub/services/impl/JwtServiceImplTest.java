package com.mchub.services.impl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtServiceImpl. No mocks needed — generates real tokens
 * with a test secret and verifies them against the same instance.
 *
 * isTokenValid() had a real NPE bug fixed during the audit: comparing
 * extractedId.equals(userId) threw NullPointerException for tokens missing
 * the "id" claim (old/malformed tokens) instead of returning false. The
 * fix switched to Objects.equals(). The "malformed token" tests below pin
 * that fix down as a regression guard.
 */
class JwtServiceImplTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs512-algorithm";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_MS);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /** Builds a token missing the "id" claim — simulates an old/malformed token. */
    private String tokenWithoutIdClaim(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "CLIENT");
        // deliberately no "id" claim
        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(signingKey())
                .compact();
    }

    @Nested
    @DisplayName("generateToken / extractUserId / extractRole")
    class GenerateAndExtract {

        @Test
        @DisplayName("round-trips userId and role through a generated token")
        void roundTripsClaims() {
            String token = jwtService.generateToken("user-123", "ADMIN");

            assertThat(jwtService.extractUserId(token)).isEqualTo("user-123");
            assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("single-arg generateToken defaults role to CLIENT")
        void defaultsRoleToClient() {
            String token = jwtService.generateToken("user-456");

            assertThat(jwtService.extractRole(token)).isEqualTo("CLIENT");
        }

        @Test
        @DisplayName("extractIssuedAt returns a timestamp close to now")
        void extractsIssuedAt() {
            long before = System.currentTimeMillis();
            String token = jwtService.generateToken("user-789");
            long after = System.currentTimeMillis();

            Date issuedAt = jwtService.extractIssuedAt(token);

            assertThat(issuedAt.getTime()).isBetween(before - 1000, after + 1000);
        }
    }

    @Nested
    @DisplayName("isTokenValid — REGRESSION GUARD for fixed NPE bug")
    class IsTokenValid {

        @Test
        @DisplayName("true for a fresh token matching the given userId")
        void trueForMatchingFreshToken() {
            String token = jwtService.generateToken("user-1");

            assertThat(jwtService.isTokenValid(token, "user-1")).isTrue();
        }

        @Test
        @DisplayName("false when userId does not match the token's id claim")
        void falseForMismatchedUserId() {
            String token = jwtService.generateToken("user-1");

            assertThat(jwtService.isTokenValid(token, "someone-else")).isFalse();
        }

        @Test
        @DisplayName("REGRESSION GUARD: returns false — does NOT throw NullPointerException — for a token missing the 'id' claim")
        void doesNotThrowForTokenMissingIdClaim() {
            String malformedToken = tokenWithoutIdClaim("user-1");

            assertThatCode(() -> jwtService.isTokenValid(malformedToken, "user-1"))
                    .doesNotThrowAnyException();
            assertThat(jwtService.isTokenValid(malformedToken, "user-1")).isFalse();
        }

        @Test
        @DisplayName("FINDING: an expired token throws ExpiredJwtException from isTokenValid() instead of returning false")
        void expiredTokenThrowsInsteadOfReturningFalse() {
            // JJWT's parseSignedClaims() throws ExpiredJwtException the moment it parses an
            // expired token — this happens inside extractUserId() (called first in
            // isTokenValid()), before isTokenExpired()'s manual date comparison is ever
            // reached. So isTokenValid() cannot return false for an expired token the way
            // its code reads like it should — it always throws for that case instead.
            //
            // This is NOT exploitable in production: isTokenValid() is dead code (grep
            // confirms nothing in src/main/java calls it). The real request-auth path,
            // JwtAuthenticationFilter, calls extractUserId()/extractRole()/extractIssuedAt()
            // directly inside its own try/catch(Exception), so an expired token there is
            // still handled safely (logged, request proceeds unauthenticated). Documented
            // here so nobody starts calling isTokenValid() on a hot path assuming it
            // degrades gracefully for expired tokens — it does not.
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", "user-1");
            claims.put("role", "CLIENT");
            String expiredToken = Jwts.builder()
                    .claims(claims)
                    .subject("user-1")
                    .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                    .expiration(new Date(System.currentTimeMillis() - 5_000)) // already expired
                    .signWith(signingKey())
                    .compact();

            assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, "user-1"))
                    .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
        }
    }
}
