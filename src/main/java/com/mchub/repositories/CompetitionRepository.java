package com.mchub.repositories;

import com.mchub.models.Competition;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionRepository extends MongoRepository<Competition, String> {
  List<Competition> findByActive(boolean active);
}
