package com.mchub.services;

import com.mchub.models.MCProfile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

public interface MCProfileService {

        @PreAuthorize("hasAuthority('ADMIN') or #userId == authentication.name")
    Map<String, Object> getDashboardStats(String userId);

        @PreAuthorize("hasAuthority('MC')")
    MCProfile updateProfile(String userId, MCProfile profileData);

}
