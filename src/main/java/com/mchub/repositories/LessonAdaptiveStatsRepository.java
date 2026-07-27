package com.mchub.repositories;

import com.mchub.models.LessonAdaptiveStats;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonAdaptiveStatsRepository
    extends MongoRepository<LessonAdaptiveStats, String> {
  Optional<LessonAdaptiveStats> findByLessonId(String lessonId);
}
