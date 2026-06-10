package com.mchub.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailCampaignResponseDTO {
    private String id;
    private String templateId;
    private String subject;
    private String status;
    private int totalRecipients;
    private int successCount;
    private int failedCount;
    private LocalDateTime createdAt;
    private String targetType;
    private List<String> targetPlans;
    private List<String> targetRoles;
    private List<String> targetEmails;
}
