package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Stores calibrated thresholds for each lesson derived from
 * aggregated practice session data. Updated asynchronously after
 * every practice session once the lesson has enough data points.
 */
@Document(collection = "lesson_adaptive_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonAdaptiveStats {

    @Id
    private String id;

    @Indexed(unique = true)
    private String lessonId;

    // Raw aggregated metrics from practice sessions
    private long sessionCount;
    private double avgOverallScore;
    private double avgAccuracyScore;
    private double avgRhythmScore;
    private double avgWpm;
    private double minOverallScore;
    private double maxOverallScore;
    private double p25OverallScore;
    private double p75OverallScore;

    // Calibrated thresholds (what AI actually uses for scoring)
    private int calibratedPassingScore;
    private int calibratedWpmMin;
    private int calibratedWpmMax;

    // Difficulty signal: "EASY" | "MEDIUM" | "HARD" | "VERY_HARD"
    private String perceivedDifficulty;

    private Instant lastCalibrated;
    private Instant createdAt;
}
