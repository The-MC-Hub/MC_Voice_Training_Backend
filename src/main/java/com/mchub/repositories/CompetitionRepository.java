package com.mchub.repositories;

import com.mchub.models.Competition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetitionRepository extends MongoRepository<Competition, String> {
    List<Competition> findByActive(boolean active);
}
