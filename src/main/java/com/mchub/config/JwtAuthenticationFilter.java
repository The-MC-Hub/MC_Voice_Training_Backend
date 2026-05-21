package com.mchub.config;

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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. Check Authorization Header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract Token
        final String jwt = authHeader.substring(7).trim();
        if (jwt.isEmpty() || jwt.equals("null") || jwt.equals("undefined")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. Decode token to get userId and role
            String userId = jwtService.extractUserId(jwt);
            String role = jwtService.extractRole(jwt); // e.g. "MC", "CLIENT", "ADMIN"

            // 4. If not yet authenticated in Context, perform authentication
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Create authority from role in JWT (e.g. "ADMIN" →
                // SimpleGrantedAuthority("ADMIN"))
                List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
                        ? Collections.singletonList(new SimpleGrantedAuthority(role))
                        : Collections.emptyList();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId, // Principal = userId (SecurityUtils.getCurrentUserId() đọc từ đây)
                        null,
                        authorities // GrantedAuthority list — required for @PreAuthorize to work
                );

                authToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(Objects.requireNonNull(request)));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT Auth OK — userId={}, role={}, path={}",
                        userId, role, request.getRequestURI());
            }
        } catch (Exception e) {
            // Token expired or invalid — log and continue (Spring Security will block if
            // route needs auth)
            log.warn("⚠️ [JWT] Token validation failed for {}: {}", request.getRequestURI(),
                    Objects.requireNonNull(e).getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
