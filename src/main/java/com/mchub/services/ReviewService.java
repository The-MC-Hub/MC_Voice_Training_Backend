package com.mchub.services;

import com.mchub.dto.CreateReviewRequest;
import com.mchub.models.Review;

import java.util.List;

public interface ReviewService {

    Review createReview(CreateReviewRequest request, String clientId);

    List<Review> getReviewsByMc(String mcId);

    void deleteReview(String reviewId);
}
