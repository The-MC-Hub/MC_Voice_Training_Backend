package com.mchub.repositories;

import com.mchub.models.SocialPost;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialPostRepository extends MongoRepository<SocialPost, String> {
    List<SocialPost> findByActiveTrueOrderBySortOrderDesc();
    List<SocialPost> findAllByOrderBySortOrderDesc();
}
