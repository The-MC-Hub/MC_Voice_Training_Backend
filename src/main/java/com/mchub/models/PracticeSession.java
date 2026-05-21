package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "practice_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSession {
    @Id
    private String id;

    @Indexed
    private String lessonId;

    @Indexed
    @Field("userId")
    private String userId;
    
    private String audioUrl;
    
    // AI Analysis results
    private String textSpoken;
    private double accuracyScore;
    private double rhythmScore;
    private double speakingRateWpm;
    private String feedbackVi;
    private String feedbackEn;
    private String reportVi;
    private String reportEn;
    private List<ExpertTip> expertTipsVi;
    private List<ExpertTip> expertTipsEn;

    /** Per-criterion weighted scores, e.g. {"PRONUNCIATION": 85.0, "PACING": 90.0} */
    private Map<String, Double> criteriaScores;

    /** Weighted overall score computed from criteriaScores + weights */
    private double overallScore;

    @Field("createdAt")
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpertTip {
        private String label;
        private String tip;
    }
}
