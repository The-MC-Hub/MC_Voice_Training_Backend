package com.mchub.models;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;


@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    @JsonIgnore
    private String password;

    
    @Builder.Default
    private UserRole role = UserRole.CLIENT;

    private String phoneNumber;

    @Builder.Default
    private String avatar = "default-avatar.png";

    @Builder.Default
    private String bio = "";

    @Builder.Default
    private boolean isVerified = false;



    @Builder.Default
    private boolean isActive = true;

    
    private String mcProfile;

    @Builder.Default
    private boolean isPremium = false;

    @Builder.Default
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    private LocalDateTime planExpiresAt;

    // AI coaching session count for current billing period (resets on renewal)
    @Builder.Default
    private int aiSessionsUsed = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
