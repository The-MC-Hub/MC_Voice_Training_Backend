package com.mchub.services;

import com.mchub.models.MCProfile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

public interface MCProfileService {

        @PreAuthorize("hasAuthority('ADMIN') or (hasAuthority('MC') and #userId == authentication.name)")
    Map<String, Object> getDashboardStats(String userId);

        @PreAuthorize("hasAuthority('MC')")
    MCProfile updateProfile(String userId, MCProfile profileData);

        @PreAuthorize("hasAuthority('MC')")
    MCProfile getOwnProfile(String userId);

}
