package com.mchub.models;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "email_templates")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailTemplate {
    @Id private String id;
    private String name;
    private String subject;
    private String htmlContent;
    private DesignData designData;
    @CreatedDate private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DesignData {
        private String logoUrl, bannerUrl, title, description, buttonText, buttonLink;
    }
}
