package com.mchub.repositories;

import com.mchub.models.PracticeSession;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PracticeSessionRepository extends MongoRepository<PracticeSession, String> {
    @Query("{'userId': ?0}")
    List<PracticeSession> findByUserId(String userId);

    @Query("{'lessonId': ?0}")
    List<PracticeSession> findByLessonId(String lessonId);

    List<PracticeSession> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("{'userId': ?0, 'lessonId': ?1}")
    List<PracticeSession> findByUserIdAndLessonId(String userId, String lessonId);

    @Query(value = "{'userId': ?0}", count = true)
    long countByUserId(String userId);

    @Query(value = "{'lessonId': ?0}", count = true)
    long countByLessonId(String lessonId);

    List<PracticeSession> findByCreatedAtAfter(java.time.Instant after);
    List<PracticeSession> findByCreatedAtBetween(java.time.Instant from, java.time.Instant to);
    long countByCreatedAtAfter(java.time.Instant after);

    // Aggregation: avg/min/max scores per lesson (requires >= minCount sessions)
    @Aggregation(pipeline = {
        "{ $match: { lessonId: ?0 } }",
        "{ $group: { _id: '$lessonId', " +
            "sessionCount:    { $sum: 1 }, " +
            "avgOverall:      { $avg: '$overallScore' }, " +
            "avgAccuracy:     { $avg: '$accuracyScore' }, " +
            "avgRhythm:       { $avg: '$rhythmScore' }, " +
            "avgWpm:          { $avg: '$speakingRateWpm' }, " +
            "minOverall:      { $min: '$overallScore' }, " +
            "maxOverall:      { $max: '$overallScore' }, " +
            "p25Overall:      { $percentile: { input: '$overallScore', p: [0.25], method: 'approximate' } }, " +
            "p75Overall:      { $percentile: { input: '$overallScore', p: [0.75], method: 'approximate' } } " +
        "} }"
    })
    Optional<LessonScoreStats> aggregateScoresByLessonId(String lessonId);

    interface LessonScoreStats {
        String get_id();
        long getSessionCount();
        double getAvgOverall();
        double getAvgAccuracy();
        double getAvgRhythm();
        double getAvgWpm();
        double getMinOverall();
        double getMaxOverall();
        // MongoDB $percentile returns array — use List via projection
        java.util.List<Double> getP25Overall();
        java.util.List<Double> getP75Overall();
    }
}
