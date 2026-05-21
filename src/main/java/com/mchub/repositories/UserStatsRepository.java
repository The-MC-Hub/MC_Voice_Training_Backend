package com.mchub.repositories;

import com.mchub.models.UserStats;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStatsRepository extends MongoRepository<UserStats, String> {
    Optional<UserStats> findByUserId(String userId);
}
