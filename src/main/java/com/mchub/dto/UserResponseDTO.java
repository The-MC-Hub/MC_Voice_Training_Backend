package com.mchub.dto;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDTO {

    private String id;
    private String name;
    private String email;
    private UserRole role;
    private String phoneNumber;
    private String avatar;
    private String bio;
    private boolean isVerified;

    private boolean isActive;
    private String mcProfile;
    private boolean isPremium;
    private SubscriptionPlan plan;
    private int aiSessionsUsed;
    private LocalDateTime planExpiresAt;
    private LocalDateTime createdAt;

}
