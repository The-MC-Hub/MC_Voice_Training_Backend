package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "certificates")
@CompoundIndex(name = "user_course_unique_idx", def = "{'userId': 1, 'courseId': 1}", unique = true, sparse = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    private String id;

    @Indexed
    private String userId;       // owner — was mcProfileId (wrong)

    private String courseId;     // which course earned this certificate

    private String courseName;   // denormalised for display

    private String userName;     // denormalised for display

    private double completionScore; // quiz score at time of completion (0–100)

    private String imageUrl;     // generated certificate image (optional)

    @Builder.Default
    private boolean isVerified = true; // auto-verified on course completion

    @CreatedDate
    private LocalDateTime issuedAt;
}
