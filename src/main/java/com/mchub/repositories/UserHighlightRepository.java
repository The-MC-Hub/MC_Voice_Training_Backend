package com.mchub.repositories;

import com.mchub.models.UserHighlight;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserHighlightRepository extends MongoRepository<UserHighlight, String> {
  List<UserHighlight> findByUserIdAndReadingGuideIdOrderByCreatedAtDesc(
      String userId, String readingGuideId);
}
