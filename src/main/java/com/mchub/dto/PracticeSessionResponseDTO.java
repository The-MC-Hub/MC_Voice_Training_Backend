package com.mchub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSessionResponseDTO {
    @com.fasterxml.jackson.annotation.JsonProperty("id")
    private String id;
    @com.fasterxml.jackson.annotation.JsonProperty("lesson_id")
    private String lessonId;
    @com.fasterxml.jackson.annotation.JsonProperty("lesson_title")
    private String lessonTitle;
    @com.fasterxml.jackson.annotation.JsonProperty("user_id")
    private String userId;
    @com.fasterxml.jackson.annotation.JsonProperty("audio_url")
    private String audioUrl;
    @com.fasterxml.jackson.annotation.JsonProperty("text_spoken")
    private String textSpoken;
    @com.fasterxml.jackson.annotation.JsonProperty("accuracy_score")
    private double accuracyScore;
    @com.fasterxml.jackson.annotation.JsonProperty("rhythm_score")
    private double rhythmScore;
    @com.fasterxml.jackson.annotation.JsonProperty("speaking_rate_wpm")
    private double speakingRateWpm;
    @com.fasterxml.jackson.annotation.JsonProperty("feedback_vi")
    private String feedbackVi;
    @com.fasterxml.jackson.annotation.JsonProperty("feedback_en")
    private String feedbackEn;
    @com.fasterxml.jackson.annotation.JsonProperty("report_vi")
    private String reportVi;
    @com.fasterxml.jackson.annotation.JsonProperty("report_en")
    private String reportEn;
    @com.fasterxml.jackson.annotation.JsonProperty("tips_vi")
    private List<ExpertTipDTO> expertTipsVi;
    @com.fasterxml.jackson.annotation.JsonProperty("tips_en")
    private List<ExpertTipDTO> expertTipsEn;
    @com.fasterxml.jackson.annotation.JsonProperty("criteria_scores")
    private java.util.Map<String, Double> criteriaScores;

    @com.fasterxml.jackson.annotation.JsonProperty("overall_score")
    private double overallScore;

    @com.fasterxml.jackson.annotation.JsonProperty("created_at")
    private java.time.Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpertTipDTO {
        private String label;
        private String tip;
    }
}
