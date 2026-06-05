package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "social_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialPost {

    @Id
    private String id;

    private String image;        // URL (Cloudinary or external)
    private String description;
    private String fbLink;
    private int sortOrder;       // higher = appears first in carousel (newest default highest)
    private boolean active;
    private long clickCount;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
