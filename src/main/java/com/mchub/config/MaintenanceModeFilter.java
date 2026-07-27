package com.mchub.config;

import com.mchub.repositories.SystemSettingRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class MaintenanceModeFilter extends OncePerRequestFilter {

  private final SystemSettingRepository systemSettingRepo;

  private static final Set<String> ALLOWED_PATHS =
      Set.of("/api/v1/auth/login", "/api/v1/admin", "/swagger-ui", "/v3/api-docs");

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    if (isAllowedPath(path)) {
      filterChain.doFilter(request, response);
      return;
    }

    boolean maintenanceMode =
        systemSettingRepo
            .findById("MAINTENANCE_MODE")
            .map(s -> "true".equalsIgnoreCase(s.getValue()))
            .orElse(false);

    if (maintenanceMode && !isAdminUser()) {
      response.setStatus(503);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response
          .getWriter()
          .write(
              "{\"status\":\"fail\",\"message\":\"Hệ thống đang bảo trì để nâng cấp. Vui lòng quay lại sau.\",\"errorCode\":\"MAINTENANCE_MODE\",\"data\":null}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean isAllowedPath(String path) {
    return ALLOWED_PATHS.stream().anyMatch(path::startsWith);
  }

  private boolean isAdminUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return false;
    return auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ADMIN") || a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
  }
}
