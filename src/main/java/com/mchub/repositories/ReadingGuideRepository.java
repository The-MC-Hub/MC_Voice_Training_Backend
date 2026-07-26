package com.mchub.repositories;

import com.mchub.models.ReadingGuide;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingGuideRepository extends MongoRepository<ReadingGuide, String> {
  List<ReadingGuide> findByCategory(String category);
}
