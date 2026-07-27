package com.mchub.services;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SystemHealthService {

  @PreAuthorize("hasAuthority('ADMIN')")
  Map<String, Object> getSystemHealth();
}
