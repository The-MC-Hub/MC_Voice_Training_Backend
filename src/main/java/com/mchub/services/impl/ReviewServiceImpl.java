package com.mchub.services.impl;

import com.mchub.dto.CreateReviewRequest;
import com.mchub.enums.BookingStatus;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Booking;
import com.mchub.models.Review;
import com.mchub.repositories.BookingRepository;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.ReviewRepository;
import com.mchub.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final MCProfileRepository mcProfileRepository;

    @Override
    public Review createReview(CreateReviewRequest request, String clientId) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getClient().equals(clientId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        boolean alreadyReviewed = reviewRepository.findByBooking(request.getBookingId())
                .stream().anyMatch(r -> r.getClient().equals(clientId));
        if (alreadyReviewed) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
                .booking(request.getBookingId())
                .mc(booking.getMc())
                .client(clientId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);
        updateMCProfileRatingAsync(booking.getMc());
        return saved;
    }

    @Override
    public List<Review> getReviewsByMc(String mcId) {
        return reviewRepository.findByMc(mcId);
    }

    @Override
    public void deleteReview(String reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    @Async
    void updateMCProfileRatingAsync(String mcId) {
        mcProfileRepository.findByUser(mcId).ifPresent(profile -> {
            List<Review> reviews = reviewRepository.findByMc(mcId);
            double avg = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            profile.setRating(Math.round(avg * 10.0) / 10.0);
            profile.setReviewsCount(reviews.size());
            mcProfileRepository.save(profile);
        });
    }
}
