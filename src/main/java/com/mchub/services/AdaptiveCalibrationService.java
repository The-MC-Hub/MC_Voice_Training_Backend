package com.mchub.services;

import com.mchub.models.LessonAdaptiveStats;
import com.mchub.models.VoiceLesson;
import com.mchub.repositories.LessonAdaptiveStatsRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.PracticeSessionRepository.LessonScoreStats;
import com.mchub.repositories.VoiceLessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Adaptive calibration — recalibrates passing score and WPM targets
 * for a lesson based on aggregated real user performance data.
 *
 * Triggered asynchronously after each practice session.
 * Only recalibrates once the lesson has reached MIN_SESSIONS data points.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdaptiveCalibrationService {

    private static final int MIN_SESSIONS = 10;

    // Max drift allowed from admin-configured values (±15 points)
    private static final int MAX_PASSING_SCORE_DRIFT = 15;
    private static final int MAX_WPM_DRIFT = 20;

    private final PracticeSessionRepository sessionRepository;
    private final LessonAdaptiveStatsRepository adaptiveStatsRepository;
    private final VoiceLessonRepository lessonRepository;

    /**
     * Called fire-and-forget after analyzePractice() completes.
     * Skips silently if not enough data yet.
     */
    @Async
    public void calibrateLesson(String lessonId) {
        try {
            long count = sessionRepository.countByLessonId(lessonId);
            if (count < MIN_SESSIONS) {
                log.debug("[Adaptive] Lesson {} has {} sessions — below threshold {}, skipping", lessonId, count, MIN_SESSIONS);
                return;
            }

            LessonScoreStats stats = sessionRepository.aggregateScoresByLessonId(lessonId).orElse(null);
            if (stats == null) {
                log.warn("[Adaptive] No aggregation result for lesson {}", lessonId);
                return;
            }

            VoiceLesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson == null) {
                log.warn("[Adaptive] Lesson {} not found during calibration", lessonId);
                return;
            }

            double p25 = extractPercentile(stats.getP25Overall());
            double p75 = extractPercentile(stats.getP75Overall());

            // Calibrated passing score = p25 score, clamped within ±15 of admin value
            int adminPassing = lesson.getPassingScore();
            int rawCalibrated = (int) Math.round(p25);
            int calibratedPassing = clamp(rawCalibrated, adminPassing - MAX_PASSING_SCORE_DRIFT, adminPassing + MAX_PASSING_SCORE_DRIFT);
            // Never let passing score fall below 40 or exceed 95
            calibratedPassing = Math.max(40, Math.min(95, calibratedPassing));

            // Calibrated WPM = center on avg WPM ± 15, clamped within ±20 of admin values
            int avgWpmInt = (int) Math.round(stats.getAvgWpm());
            int calWpmMin = clamp(avgWpmInt - 15, lesson.getTargetWpmMin() - MAX_WPM_DRIFT, lesson.getTargetWpmMin() + MAX_WPM_DRIFT);
            int calWpmMax = clamp(avgWpmInt + 15, lesson.getTargetWpmMax() - MAX_WPM_DRIFT, lesson.getTargetWpmMax() + MAX_WPM_DRIFT);
            calWpmMin = Math.max(60, calWpmMin);
            calWpmMax = Math.max(calWpmMin + 10, calWpmMax);

            // Perceived difficulty based on avg score
            String difficulty = classifyDifficulty(stats.getAvgOverall(), p25, p75);

            LessonAdaptiveStats existing = adaptiveStatsRepository.findByLessonId(lessonId).orElse(null);
            LessonAdaptiveStats updated = LessonAdaptiveStats.builder()
                    .id(existing != null ? existing.getId() : null)
                    .lessonId(lessonId)
                    .sessionCount(stats.getSessionCount())
                    .avgOverallScore(stats.getAvgOverall())
                    .avgAccuracyScore(stats.getAvgAccuracy())
                    .avgRhythmScore(stats.getAvgRhythm())
                    .avgWpm(stats.getAvgWpm())
                    .minOverallScore(stats.getMinOverall())
                    .maxOverallScore(stats.getMaxOverall())
                    .p25OverallScore(p25)
                    .p75OverallScore(p75)
                    .calibratedPassingScore(calibratedPassing)
                    .calibratedWpmMin(calWpmMin)
                    .calibratedWpmMax(calWpmMax)
                    .perceivedDifficulty(difficulty)
                    .lastCalibrated(Instant.now())
                    .createdAt(existing != null ? existing.getCreatedAt() : Instant.now())
                    .build();

            adaptiveStatsRepository.save(updated);
            log.info("[Adaptive] Lesson {} calibrated — sessions={}, avgScore={:.1f}, passing={}->{}, difficulty={}",
                    lessonId, stats.getSessionCount(), stats.getAvgOverall(), adminPassing, calibratedPassing, difficulty);

        } catch (Exception e) {
            log.error("[Adaptive] Calibration failed for lesson {}: {}", lessonId, e.getMessage());
        }
    }

    private double extractPercentile(List<Double> pctList) {
        if (pctList != null && !pctList.isEmpty() && pctList.get(0) != null) {
            return pctList.get(0);
        }
        return 0.0;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String classifyDifficulty(double avg, double p25, double p75) {
        // High avg + tight spread = lesson is easy
        if (avg >= 80 && p25 >= 65) return "EASY";
        if (avg >= 65 && p25 >= 50) return "MEDIUM";
        if (avg >= 50)              return "HARD";
        return "VERY_HARD";
    }
}
