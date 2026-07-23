package com.mchub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("isVerified")
    private boolean isVerified;

    @JsonProperty("isActive")
    private boolean isActive;
    private String mcProfile;

    @JsonProperty("isPremium")
    private boolean isPremium;
    private SubscriptionPlan plan;
    private int aiSessionsUsed;
    private LocalDateTime planExpiresAt;
    private LocalDateTime createdAt;
    private String referralCode;
    private int referralCount;

    @JsonProperty("isGoogleLinked")
    private boolean isGoogleLinked;

}
