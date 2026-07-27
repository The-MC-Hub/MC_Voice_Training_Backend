package com.mchub.repositories;

import com.mchub.models.SocialPost;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialPostRepository extends MongoRepository<SocialPost, String> {
  List<SocialPost> findByActiveTrueOrderBySortOrderDesc();

  List<SocialPost> findAllByOrderBySortOrderDesc();
}
