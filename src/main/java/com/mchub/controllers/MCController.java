package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.models.MCProfile;
import com.mchub.services.MCProfileService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/mcs")
@RequiredArgsConstructor
public class MCController {

    private final MCProfileService mcProfileService;

        @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        String userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> stats = mcProfileService.getDashboardStats(Objects.requireNonNull(userId));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

        @PutMapping("/profile")
    public ResponseEntity<ApiResponse<MCProfile>> updateProfile(@RequestBody MCProfile profileData) {
        String userId = SecurityUtils.getCurrentUserId();
        MCProfile updated = mcProfileService.updateProfile(Objects.requireNonNull(userId),
                Objects.requireNonNull(profileData));
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

}
