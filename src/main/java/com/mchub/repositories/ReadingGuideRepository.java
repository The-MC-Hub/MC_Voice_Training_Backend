package com.mchub.repositories;

import com.mchub.models.ReadingGuide;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingGuideRepository extends MongoRepository<ReadingGuide, String> {
    List<ReadingGuide> findByCategory(String category);
}
