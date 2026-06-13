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
}
