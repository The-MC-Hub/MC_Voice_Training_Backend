package com.mchub.config;

import com.mchub.models.User;
import com.mchub.repositories.UserRepository;
import com.mchub.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Extract token from Authorization header only (never from query params — logs would expose it)
        String jwt;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7).trim();
        } else {
            filterChain.doFilter(request, response);
            return;
        }
        if (jwt.isEmpty() || jwt.equals("null") || jwt.equals("undefined")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String userId = jwtService.extractUserId(jwt);
            String role   = jwtService.extractRole(jwt);
            Date issuedAt = jwtService.extractIssuedAt(jwt);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Reject token if password was changed after it was issued
                if (issuedAt != null) {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null && user.getPasswordChangedAt() != null) {
                        long pwChangedMs = user.getPasswordChangedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
                        if (issuedAt.getTime() < pwChangedMs) {
                            log.warn("⚠️ [JWT] Token issued before password change — userId={}", userId);
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }
                }

                List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
                        ? Collections.singletonList(new SimpleGrantedAuthority(role))
                        : Collections.emptyList();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(Objects.requireNonNull(request)));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT Auth OK — userId={}, role={}, path={}", userId, role, request.getRequestURI());
            }
        } catch (Exception e) {
            log.warn("⚠️ [JWT] Token validation failed for {}: {}", request.getRequestURI(),
                    Objects.requireNonNull(e).getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
