package com.mchub.services.impl;

import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.services.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${mchub.jwt.secret}")
    private String secretKey;

    @Value("${mchub.jwt.expiration}")
    private long jwtExpiration;

    @PostConstruct
    private void validateSecret() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET environment variable is not set. Refusing to start with no signing key.");
        }
        if (secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short (" + secretKey.length()
                            + " chars) — need at least 32 bytes for HS256. Generate a proper random secret.");
        }
    }

    @Override
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("id", String.class));
    }

    @Override
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    @Override
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    @Override
    public String generateToken(String userId, String role) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", userId);
        extraClaims.put("role", role);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    @Override
    public String generateToken(String userId) {
        return generateToken(userId, "CLIENT");
    }

    @Override
    public boolean isTokenValid(String token, String userId) {
        try {
            final String extractedId = extractUserId(token);
            return java.util.Objects.equals(extractedId, userId) && !isTokenExpired(token);
        } catch (io.jsonwebtoken.JwtException e) {
            // Expired/malformed/tampered token — not valid, not an error condition for this method.
            return false;
        }
    }

    private static final long PENDING_GOOGLE_TOKEN_TTL_MS = 10 * 60 * 1000; // 10 minutes

    @Override
    public String generatePendingGoogleToken(String googleId, String email, String name) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("pendingGoogleId", googleId);
        claims.put("pendingGoogleEmail", email);
        claims.put("pendingGoogleName", name != null ? name : "");

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + PENDING_GOOGLE_TOKEN_TTL_MS))
                .signWith(getSignInKey())
                .compact();
    }

    @Override
    public PendingGoogleIdentity extractPendingGoogleIdentity(String pendingToken) {
        final Claims claims;
        try {
            claims = extractAllClaims(pendingToken);
        } catch (JwtException e) {
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID, "Pending registration token is invalid or expired.");
        }
        String googleId = claims.get("pendingGoogleId", String.class);
        String email = claims.get("pendingGoogleEmail", String.class);
        String name = claims.get("pendingGoogleName", String.class);
        if (googleId == null || email == null) {
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID, "Pending registration token is invalid or expired.");
        }
        return new PendingGoogleIdentity(googleId, email, name);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
