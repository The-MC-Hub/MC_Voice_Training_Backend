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

    /** Phase 1 extended accuracy metrics */
    private double cerRate;
    private double werRate;

    /** Phase 2 — Spectral + articulation features (from AI) */
    /** { spectral_centroid_hz, spectral_contrast_mean, mfcc_stability_score } */
    private Map<String, Object> spectralFeatures;

    /** Phase 2 — Pitch contour direction { pitch_slope, pitch_contour } */
    private Map<String, Object> pitchContour;

    /** Phase 2 — Filler word detection { filler_count, filler_ratio, filler_score, fillers_found } */
    private Map<String, Object> fillerWords;

    /** Phase 3 — Voice quality { jitter_pct, shimmer_pct, hnr_db } */
    private Map<String, Object> voiceQuality;

    /** Emotion breakdown { emotion_score, pitch_score, energy_std_score, tempo_var_score } */
    private Map<String, Object> emotionBreakdown;

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
