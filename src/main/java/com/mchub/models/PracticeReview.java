package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "practice_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeReview {

    @Id
    private String id;

    @Indexed
    private String practiceSessionId;

    @Indexed
    private String revieweeId; // the learner who recorded the practice session

    @Indexed
    private String reviewerId; // null while pending — set when an MC claims it

    private String comment;

    private Integer rating; // 1-5, null until reviewed

    @Builder.Default
    private String status = "PENDING"; // PENDING | REVIEWED

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}
