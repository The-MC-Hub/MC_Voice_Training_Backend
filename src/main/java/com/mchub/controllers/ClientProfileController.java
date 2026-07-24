package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.ClientProfileDTO;
import com.mchub.services.ClientProfileService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
public class ClientProfileController {

    private final ClientProfileService clientProfileService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ClientProfileDTO>> getProfile() {
        String userId = SecurityUtils.getCurrentUserId();
        ClientProfileDTO profile = clientProfileService.getProfile(Objects.requireNonNull(userId));
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ClientProfileDTO>> updateProfile(@RequestBody ClientProfileDTO profileData) {
        String userId = SecurityUtils.getCurrentUserId();
        ClientProfileDTO updated = clientProfileService.updateProfile(Objects.requireNonNull(userId), profileData);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<ClientProfileDTO>> createProfile(@RequestBody ClientProfileDTO profileData) {
        String userId = SecurityUtils.getCurrentUserId();
        ClientProfileDTO created = clientProfileService.createProfile(Objects.requireNonNull(userId), profileData);
        return ResponseEntity.ok(ApiResponse.success("Profile created successfully", created));
    }
}
