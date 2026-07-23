package com.mchub.dto;

import com.mchub.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SendNotificationRequest {

    // "USER" | "ROLE" | "ALL"
    @NotBlank(message = "targetType cannot be empty")
    private String targetType;

    // Required when targetType = USER
    private String userId;

    // Required when targetType = ROLE (CLIENT | MC | ADMIN)
    private String role;

    @NotNull(message = "type cannot be empty")
    private NotificationType type;

    @NotBlank(message = "title cannot be empty")
    private String title;

    @NotBlank(message = "body cannot be empty")
    private String body;

    private String actionUrl;
}
