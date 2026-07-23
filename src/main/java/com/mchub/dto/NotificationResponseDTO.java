package com.mchub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mchub.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponseDTO {

    private String id;
    private String userId;
    private NotificationType type;
    private String title;
    private String body;
    private String actionUrl;

    @JsonProperty("isRead")
    private boolean isRead;

    private LocalDateTime createdAt;
}
