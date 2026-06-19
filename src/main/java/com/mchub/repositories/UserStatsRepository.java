package com.mchub.repositories;

import com.mchub.models.UserStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStatsRepository extends MongoRepository<UserStats, String> {
    Optional<UserStats> findByUserId(String userId);

    Page<UserStats> findAllByOrderByTotalPracticeHoursDesc(Pageable pageable);
    Page<UserStats> findAllByOrderByCumulativeXPDesc(Pageable pageable);
    Page<UserStats> findAllByOrderByCurrentStreakDesc(Pageable pageable);
    Page<UserStats> findAllByOrderByWeeklyXPDesc(Pageable pageable);
    Page<UserStats> findAllByOrderByTotalSessionsDesc(Pageable pageable);

    long countByTotalPracticeHoursGreaterThan(double value);
    long countByCumulativeXPGreaterThan(double value);
    long countByCurrentStreakGreaterThan(int value);
}
