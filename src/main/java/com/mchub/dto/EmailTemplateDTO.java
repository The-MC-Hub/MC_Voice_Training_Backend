package com.mchub.dto;

import com.mchub.models.EmailTemplate;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailTemplateDTO {
    private String id;
    private String name;
    private String subject;
    private String htmlContent;
    private EmailTemplate.DesignData designData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
