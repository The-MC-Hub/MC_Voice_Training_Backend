package com.mchub.models;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "email_campaigns")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailCampaign {
    @Id private String id;
    private String templateId;
    private String subject;
    @Builder.Default private String status = "PENDING";
    @Builder.Default private int totalRecipients = 0;
    @Builder.Default private int successCount = 0;
    @Builder.Default private int failedCount = 0;
    @CreatedDate private LocalDateTime createdAt;

    // Targeting
    // "ALL" | "PLAN" | "ROLE" | "PREMIUM" | "CUSTOM"
    @Builder.Default private String targetType = "ALL";
    private List<String> targetPlans;   // e.g. ["FREE","BASIC"]
    private List<String> targetRoles;   // e.g. ["CLIENT","MC"]
    private List<String> targetEmails;  // custom email list
}
