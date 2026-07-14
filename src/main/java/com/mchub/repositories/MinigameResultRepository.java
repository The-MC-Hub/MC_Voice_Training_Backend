package com.mchub.repositories;

import com.mchub.models.MinigameResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MinigameResultRepository extends MongoRepository<MinigameResult, String> {

    List<MinigameResult> findByUserIdAndGameTypeOrderByCreatedAtDesc(String userId, String gameType);

    @Query(value = "{'userId': ?0, 'gameType': ?1}", count = true)
    long countByUserIdAndGameType(String userId, String gameType);

    // Best single run for a user, used to detect personal-best on submit.
    MinigameResult findTopByUserIdAndGameTypeOrderByScoreDesc(String userId, String gameType);
}
