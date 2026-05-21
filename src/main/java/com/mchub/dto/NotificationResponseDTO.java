package com.mchub.dto;

import com.mchub.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponseDTO {

    private String id;
    private String title;
    private String body;
    private NotificationType type;
    private String relatedId;
    private String relatedModel;
    private String linkAction;
    private boolean isRead;
    private LocalDateTime createdAt;

}
