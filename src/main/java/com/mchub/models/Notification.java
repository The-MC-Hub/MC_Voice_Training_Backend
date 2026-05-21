package com.mchub.models;

import com.mchub.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;


@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    
    @Indexed
    private String user;

    
    private String senderId;

    private String title;

    private String body;

    
    private NotificationType type;

    
    private String relatedId;

    
    private String relatedModel;

    
    private String linkAction;

    @Builder.Default
    private boolean isRead = false;

    @Indexed
    @CreatedDate
    private LocalDateTime createdAt;
}
