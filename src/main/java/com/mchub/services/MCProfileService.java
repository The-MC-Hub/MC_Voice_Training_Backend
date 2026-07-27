package com.mchub.services;

import com.mchub.models.MCProfile;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MCProfileService {

  @PreAuthorize("hasAuthority('ADMIN') or (hasAuthority('MC') and #userId == authentication.name)")
  Map<String, Object> getDashboardStats(String userId);

  @PreAuthorize("hasAuthority('MC')")
  MCProfile updateProfile(String userId, MCProfile profileData);

  @PreAuthorize("hasAuthority('MC')")
  MCProfile getOwnProfile(String userId);
}
