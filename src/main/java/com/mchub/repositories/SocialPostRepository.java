package com.mchub.repositories;

import com.mchub.models.SocialPost;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SocialPostRepository extends MongoRepository<SocialPost, String> {
    List<SocialPost> findByActiveTrueOrderBySortOrderDesc();
    List<SocialPost> findAllByOrderBySortOrderDesc();
}
