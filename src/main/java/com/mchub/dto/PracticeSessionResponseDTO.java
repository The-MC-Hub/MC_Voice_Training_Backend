package com.mchub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSessionResponseDTO {
    @JsonProperty("id")
    private String id;
    @JsonProperty("lesson_id")
    private String lessonId;
    @JsonProperty("lesson_title")
    private String lessonTitle;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("audio_url")
    private String audioUrl;
    @JsonProperty("text_spoken")
    private String textSpoken;
    @JsonProperty("accuracy_score")
    private double accuracyScore;
    @JsonProperty("rhythm_score")
    private double rhythmScore;
    @JsonProperty("speaking_rate_wpm")
    private double speakingRateWpm;
    @JsonProperty("feedback_vi")
    private String feedbackVi;
    @JsonProperty("feedback_en")
    private String feedbackEn;
    @JsonProperty("report_vi")
    private String reportVi;
    @JsonProperty("report_en")
    private String reportEn;
    @JsonProperty("tips_vi")
    private List<ExpertTipDTO> expertTipsVi;
    @JsonProperty("tips_en")
    private List<ExpertTipDTO> expertTipsEn;
    @JsonProperty("criteria_scores")
    private Map<String, Double> criteriaScores;

    @JsonProperty("overall_score")
    private double overallScore;

    @JsonProperty("lesson_content")
    private String lessonContent;

    @JsonProperty("lesson_category")
    private String lessonCategory;

    @JsonProperty("lesson_difficulty")
    private String lessonDifficulty;

    @JsonProperty("lesson_description")
    private String lessonDescription;

    @JsonProperty("cer_rate")
    private double cerRate;

    @JsonProperty("wer_rate")
    private double werRate;

    @JsonProperty("spectral_features")
    private Map<String, Object> spectralFeatures;

    @JsonProperty("pitch_contour")
    private Map<String, Object> pitchContour;

    @JsonProperty("filler_words")
    private Map<String, Object> fillerWords;

    @JsonProperty("voice_quality")
    private Map<String, Object> voiceQuality;

    @JsonProperty("emotion_breakdown")
    private Map<String, Object> emotionBreakdown;

    @JsonProperty("word_alignment")
    private List<Map<String, Object>> wordAlignment;

    @JsonProperty("sentence_feedback")
    private List<Map<String, Object>> sentenceFeedback;

    @JsonProperty("created_at")
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpertTipDTO {
        private String label;
        private String tip;
    }
}
