package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_highlights")
public class UserHighlight {
    @Id
    private String id;
    
    private String userId;
    private String readingGuideId;
    
    private String selectedText;
    private String colorHex;
    private String noteContent;
    
    @Builder.Default
    private Date createdAt = new Date();
    @Builder.Default
    private Date updatedAt = new Date();
}
