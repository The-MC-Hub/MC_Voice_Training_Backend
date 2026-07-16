package com.mchub.repositories;

import com.mchub.models.PracticeSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PracticeSessionRepository extends MongoRepository<PracticeSession, String> {
    @Query("{'userId': ?0}")
    List<PracticeSession> findByUserId(String userId);

    List<PracticeSession> findByUserIdIn(List<String> userIds);

    @Query("{'lessonId': ?0}")
    List<PracticeSession> findByLessonId(String lessonId);

    List<PracticeSession> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("{'userId': ?0, 'lessonId': ?1}")
    List<PracticeSession> findByUserIdAndLessonId(String userId, String lessonId);

    @Query(value = "{'userId': ?0}", count = true)
    long countByUserId(String userId);

    @Query(value = "{'lessonId': ?0}", count = true)
    long countByLessonId(String lessonId);

    List<PracticeSession> findByCreatedAtAfter(Instant after);
    List<PracticeSession> findByCreatedAtBetween(Instant from, Instant to);
    long countByCreatedAtAfter(Instant after);

    // Per-lesson score aggregation (avg/min/max/percentiles) is computed via
    // MongoTemplate.aggregate() in AdaptiveCalibrationService instead of a
    // @Aggregation interface-projection method here — Spring Data MongoDB's
    // interface projection support for grouped/aggregated queries has a known
    // runtime gap (fails with NullPointerException on Class.isEnum() despite
    // compiling cleanly; the same issue was hit and fixed on
    // MinigameResultRepository.getLeaderboard()).
}
