package com.mchub.repositories;

import com.mchub.models.PracticeReview;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PracticeReviewRepository extends MongoRepository<PracticeReview, String> {

    List<PracticeReview> findByStatus(String status);

    List<PracticeReview> findByRevieweeId(String revieweeId);

    List<PracticeReview> findByReviewerId(String reviewerId);

    Optional<PracticeReview> findByPracticeSessionId(String practiceSessionId);

    boolean existsByPracticeSessionId(String practiceSessionId);
}
