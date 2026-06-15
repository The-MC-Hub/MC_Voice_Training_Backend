package com.mchub.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Per-IP buckets — evicted lazily (acceptable for short-lived attack windows)
    private final ConcurrentHashMap<String, Bucket> loginBuckets   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> otpBuckets     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (!"POST".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractIp(request);
        Bucket bucket = null;

        if (path.equals("/api/v1/auth/login")) {
            // 20 attempts per 15 minutes per IP
            bucket = loginBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.builder()
                        .capacity(20)
                        .refillIntervally(20, Duration.ofMinutes(15))
                        .build())
                    .build());

        } else if (path.equals("/api/v1/auth/verify-otp")
                || path.equals("/api/v1/auth/verify-admin-login-otp")) {
            // 20 attempts per 5 minutes per IP
            bucket = otpBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.builder()
                        .capacity(20)
                        .refillIntervally(20, Duration.ofMinutes(5))
                        .build())
                    .build());

        } else if (path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/resend-otp")) {
            // 20 attempts per hour per IP
            bucket = registerBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.builder()
                        .capacity(20)
                        .refillIntervally(20, Duration.ofHours(1))
                        .build())
                    .build());
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"status\":\"fail\",\"message\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau.\",\"data\":null}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
