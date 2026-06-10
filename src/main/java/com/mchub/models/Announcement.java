package com.mchub.models;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "announcements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @Id
    private String id;

    private String title;
    private String content;   // email body (plain text)
    private String emailSubject;

    private AnnouncementType type;

    public enum AnnouncementType {
        NEW_LESSON,
        DISCOUNT,
        MAINTENANCE,
        SOCIAL_POST,
        GENERAL,
        FEATURE_UPDATE,
        COMPETITION
    }

    private AnnouncementStatus status;

    public enum AnnouncementStatus {
        DRAFT,    // pending admin approval
        SENT,     // email dispatched
        CANCELLED
    }

    // Audience filter — empty = all active users
    private List<String> targetPlans;   // e.g. ["FREE","BASIC"] — empty = everyone
    private List<String> recipientIds;  // explicit user IDs — if set, overrides targetPlans

    private String createdBy;   // admin userId
    private int recipientCount; // set when sent

    private LocalDateTime sentAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Optional metadata for trigger context (e.g. lessonId, discountCode)
    private String refId;
    private String refType;
}
