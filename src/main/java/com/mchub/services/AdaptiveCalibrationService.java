package com.mchub.services;

import com.mchub.models.LessonAdaptiveStats;
import com.mchub.models.VoiceLesson;
import com.mchub.repositories.LessonAdaptiveStatsRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.VoiceLessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

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
    private final MongoTemplate mongoTemplate;

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

            Document stats = aggregateScoresByLessonId(lessonId);
            if (stats == null) {
                log.warn("[Adaptive] No aggregation result for lesson {}", lessonId);
                return;
            }

            VoiceLesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson == null) {
                log.warn("[Adaptive] Lesson {} not found during calibration", lessonId);
                return;
            }

            long sessionCount = stats.get("sessionCount", Number.class).longValue();
            double avgOverall = stats.get("avgOverall", Number.class).doubleValue();
            double avgAccuracy = stats.get("avgAccuracy", Number.class).doubleValue();
            double avgRhythm = stats.get("avgRhythm", Number.class).doubleValue();
            double avgWpm = stats.get("avgWpm", Number.class).doubleValue();
            double minOverall = stats.get("minOverall", Number.class).doubleValue();
            double maxOverall = stats.get("maxOverall", Number.class).doubleValue();
            double p25 = extractPercentile(stats, "p25Overall");
            double p75 = extractPercentile(stats, "p75Overall");

            // Calibrated passing score = p25 score, clamped within ±15 of admin value
            int adminPassing = lesson.getPassingScore();
            int rawCalibrated = (int) Math.round(p25);
            int calibratedPassing = clamp(rawCalibrated, adminPassing - MAX_PASSING_SCORE_DRIFT, adminPassing + MAX_PASSING_SCORE_DRIFT);
            // Never let passing score fall below 40 or exceed 95
            calibratedPassing = Math.max(40, Math.min(95, calibratedPassing));

            // Calibrated WPM = center on avg WPM ± 15, clamped within ±20 of admin values
            int avgWpmInt = (int) Math.round(avgWpm);
            int calWpmMin = clamp(avgWpmInt - 15, lesson.getTargetWpmMin() - MAX_WPM_DRIFT, lesson.getTargetWpmMin() + MAX_WPM_DRIFT);
            int calWpmMax = clamp(avgWpmInt + 15, lesson.getTargetWpmMax() - MAX_WPM_DRIFT, lesson.getTargetWpmMax() + MAX_WPM_DRIFT);
            calWpmMin = Math.max(60, calWpmMin);
            calWpmMax = Math.max(calWpmMin + 10, calWpmMax);

            // Perceived difficulty based on avg score
            String difficulty = classifyDifficulty(avgOverall, p25, p75);

            LessonAdaptiveStats existing = adaptiveStatsRepository.findByLessonId(lessonId).orElse(null);
            LessonAdaptiveStats updated = LessonAdaptiveStats.builder()
                    .id(existing != null ? existing.getId() : null)
                    .lessonId(lessonId)
                    .sessionCount(sessionCount)
                    .avgOverallScore(avgOverall)
                    .avgAccuracyScore(avgAccuracy)
                    .avgRhythmScore(avgRhythm)
                    .avgWpm(avgWpm)
                    .minOverallScore(minOverall)
                    .maxOverallScore(maxOverall)
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
                    lessonId, sessionCount, avgOverall, adminPassing, calibratedPassing, difficulty);

        } catch (Exception e) {
            log.error("[Adaptive] Calibration failed for lesson {}: {}", lessonId, e.getMessage());
        }
    }

    /**
     * Avg/min/max/percentile scores for a lesson, computed via MongoTemplate
     * directly rather than a @Aggregation interface-projection repository
     * method — Spring Data MongoDB's interface projection support for
     * grouped/aggregated queries has a known runtime gap (fails with
     * NullPointerException on Class.isEnum() despite compiling cleanly; the
     * same issue was previously hit and fixed on
     * MinigameResultRepository.getLeaderboard()).
     */
    private Document aggregateScoresByLessonId(String lessonId) {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("lessonId").is(lessonId)),
                group("lessonId")
                        .count().as("sessionCount")
                        .avg("overallScore").as("avgOverall")
                        .avg("accuracyScore").as("avgAccuracy")
                        .avg("rhythmScore").as("avgRhythm")
                        .avg("speakingRateWpm").as("avgWpm")
                        .min("overallScore").as("minOverall")
                        .max("overallScore").as("maxOverall")
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "practice_sessions", Document.class);
        Document base = results.getUniqueMappedResult();
        if (base == null) {
            return null;
        }

        // Percentiles require a separate raw pipeline ($percentile isn't exposed
        // by the typed Aggregation DSL) — run it directly against the driver.
        List<Document> pipeline = List.of(
                new Document("$match", new Document("lessonId", lessonId)),
                new Document("$group", new Document("_id", "$lessonId")
                        .append("p25Overall", new Document("$percentile",
                                new Document("input", "$overallScore").append("p", List.of(0.25)).append("method", "approximate")))
                        .append("p75Overall", new Document("$percentile",
                                new Document("input", "$overallScore").append("p", List.of(0.75)).append("method", "approximate"))))
        );
        Document percentiles = mongoTemplate.getCollection("practice_sessions")
                .aggregate(pipeline, Document.class)
                .first();

        if (percentiles != null) {
            base.put("p25Overall", percentiles.get("p25Overall"));
            base.put("p75Overall", percentiles.get("p75Overall"));
        }
        return base;
    }

    @SuppressWarnings("unchecked")
    private double extractPercentile(Document stats, String field) {
        Object raw = stats.get(field);
        if (raw instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
            return ((Number) list.get(0)).doubleValue();
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
