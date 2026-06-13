package com.mchub.models;

import com.mchub.enums.VoiceLessonCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "voice_lessons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceLesson {

    @Id
    private String id;

    private String title;
    private String content;
    private VoiceLessonCategory category;
    private String difficulty;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;

    // ── Evaluation configuration ──────────────────────────────────

    /** Per-criterion weights for AI scoring. Weights should sum to 100. */
    @Builder.Default
    private List<EvaluationCriteria> evaluationCriteria = new ArrayList<>();

    /** Target WPM range for this lesson type */
    @Builder.Default
    private int targetWpmMin = 120;

    @Builder.Default
    private int targetWpmMax = 150;

    /** Extra context sent to the AI evaluator (e.g. "Wedding tone, warm and emotional") */
    private String evaluationHint;

    /** Minimum weighted score to pass this lesson (overrides global default) */
    @Builder.Default
    private int passingScore = 70;

    /** Total number of times this lesson has been practiced (incremented on each analyze call) */
    @Builder.Default
    private int practiceCount = 0;

    @Builder.Default
    private boolean isActive = true;

    // ── Timestamps ────────────────────────────────────────────────

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ── Embedded ──────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationCriteria {
        /** PRONUNCIATION | RHYTHM | PACING | EMOTION | ACCURACY */
        private String aspect;
        /** 0–100, all criteria weights should sum to 100 */
        private int weight;
        /** Human-readable hint used in the AI evaluation prompt */
        private String description;
    }
}
