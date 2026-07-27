package com.mchub.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchub.util.ClientIpResolver;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final ObjectMapper JSON = new ObjectMapper();

  // Bounded Caffeine caches with TTL eviction to prevent memory leak
  private final Cache<String, Bucket> loginBuckets =
      Caffeine.newBuilder().expireAfterAccess(2, TimeUnit.HOURS).maximumSize(10000).build();
  private final Cache<String, Bucket> otpBuckets =
      Caffeine.newBuilder().expireAfterAccess(2, TimeUnit.HOURS).maximumSize(10000).build();
  private final Cache<String, Bucket> registerBuckets =
      Caffeine.newBuilder().expireAfterAccess(2, TimeUnit.HOURS).maximumSize(10000).build();
  private final Cache<String, Bucket> sensitiveBuckets =
      Caffeine.newBuilder().expireAfterAccess(2, TimeUnit.HOURS).maximumSize(10000).build();

  // Per-email buckets — closes the gap where an attacker rotates IPs to bypass per-IP limits
  // on OTP verification/resend for a single victim account.
  private final Cache<String, Bucket> otpEmailBuckets =
      Caffeine.newBuilder().expireAfterAccess(2, TimeUnit.HOURS).maximumSize(10000).build();

  private static final Set<String> OTP_PATHS =
      Set.of(
          "/api/v1/auth/verify-otp",
          "/api/v1/auth/verify-admin-login-otp",
          "/api/v1/auth/resend-otp");

  // Non-auth endpoints that are expensive (AI compute) or abuse-prone (discount probing)
  // and were previously completely unthrottled.
  private static final Set<String> SENSITIVE_PATHS =
      Set.of("/api/v1/voice/practice/analyze-guest", "/api/v1/payment/apply-discount");

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    String method = request.getMethod();

    if (!"POST".equalsIgnoreCase(method)) {
      filterChain.doFilter(request, response);
      return;
    }

    String ip = ClientIpResolver.resolve(request);
    Bucket bucket = null;

    if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/google")) {
      // 20 attempts per 15 minutes per IP
      bucket =
          loginBuckets.get(
              ip,
              k ->
                  Bucket.builder()
                      .addLimit(
                          Bandwidth.builder()
                              .capacity(20)
                              .refillIntervally(20, Duration.ofMinutes(15))
                              .build())
                      .build());

    } else if (path.equals("/api/v1/auth/google/complete-registration")) {
      // 20 attempts per hour per IP — same tier as password-based registration
      bucket =
          registerBuckets.get(
              ip,
              k ->
                  Bucket.builder()
                      .addLimit(
                          Bandwidth.builder()
                              .capacity(20)
                              .refillIntervally(20, Duration.ofHours(1))
                              .build())
                      .build());

    } else if (path.equals("/api/v1/auth/verify-otp")
        || path.equals("/api/v1/auth/verify-admin-login-otp")) {
      // 20 attempts per 5 minutes per IP
      bucket =
          otpBuckets.get(
              ip,
              k ->
                  Bucket.builder()
                      .addLimit(
                          Bandwidth.builder()
                              .capacity(20)
                              .refillIntervally(20, Duration.ofMinutes(5))
                              .build())
                      .build());

    } else if (path.equals("/api/v1/auth/register")) {
      // 20 attempts per hour per IP
      bucket =
          registerBuckets.get(
              ip,
              k ->
                  Bucket.builder()
                      .addLimit(
                          Bandwidth.builder()
                              .capacity(20)
                              .refillIntervally(20, Duration.ofHours(1))
                              .build())
                      .build());
    } else if (SENSITIVE_PATHS.contains(path)) {
      // 10 attempts per 10 minutes per IP — expensive/abuse-prone but not auth-critical
      bucket =
          sensitiveBuckets.get(
              ip,
              k ->
                  Bucket.builder()
                      .addLimit(
                          Bandwidth.builder()
                              .capacity(10)
                              .refillIntervally(10, Duration.ofMinutes(10))
                              .build())
                      .build());
    }

    if (bucket != null && !bucket.tryConsume(1)) {
      rejectTooManyRequests(response);
      return;
    }

    // Per-email OTP throttle: request body must be read via a caching wrapper so the
    // downstream controller can still read it afterwards.
    if (OTP_PATHS.contains(path)) {
      ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
      // Force body read so it gets cached before we inspect it.
      wrapped.getParameterMap();
      String email = extractEmail(wrapped);
      if (email != null && !email.isBlank()) {
        Bucket emailBucket =
            otpEmailBuckets.get(
                email.toLowerCase(),
                k ->
                    Bucket.builder()
                        .addLimit(
                            Bandwidth.builder()
                                .capacity(10)
                                .refillIntervally(10, Duration.ofMinutes(10))
                                .build())
                        .build());
        if (!emailBucket.tryConsume(1)) {
          rejectTooManyRequests(response);
          return;
        }
      }
      filterChain.doFilter(wrapped, response);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private String extractEmail(ContentCachingRequestWrapper wrapped) {
    try {
      byte[] body = wrapped.getContentAsByteArray();
      if (body.length == 0) return null;
      JsonNode node = JSON.readTree(body);
      JsonNode emailNode = node.get("email");
      return emailNode != null ? emailNode.asText() : null;
    } catch (Exception e) {
      return null;
    }
  }

  private void rejectTooManyRequests(HttpServletResponse response) throws IOException {
    response.setStatus(429);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            "{\"status\":\"fail\",\"message\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau.\",\"data\":null}");
  }
}
