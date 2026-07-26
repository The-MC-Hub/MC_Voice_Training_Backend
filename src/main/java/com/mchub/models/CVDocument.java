package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "cv_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CVDocument {

    @Id
    private String id;

    private String userId;
    private String fileName;
    private String fileUrl;
    private long fileSizeBytes;

    @CreatedDate
    private LocalDateTime uploadedAt;
}
