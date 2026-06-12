package com.mchub.repositories;

import com.mchub.models.UserHighlight;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserHighlightRepository extends MongoRepository<UserHighlight, String> {
    List<UserHighlight> findByUserIdAndReadingGuideIdOrderByCreatedAtDesc(String userId, String readingGuideId);
}
