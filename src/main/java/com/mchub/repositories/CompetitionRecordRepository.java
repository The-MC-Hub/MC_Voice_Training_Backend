package com.mchub.repositories;

import com.mchub.models.CompetitionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionRecordRepository extends MongoRepository<CompetitionRecord, String> {
    List<CompetitionRecord> findByCompetitionId(String competitionId);
    Optional<CompetitionRecord> findByCompetitionIdAndUserId(String competitionId, String userId);
}
