package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.models.Review;
import com.mchub.repositories.UserRepository;
import com.mchub.services.ReviewService;
import com.mchub.mapper.ReviewMapper;
import com.mchub.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> createReview(
            @RequestBody @Valid CreateReviewRequest req) {
        String clientId = SecurityUtils.getCurrentUserId();
        Review saved = reviewService.createReview(req, clientId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", enrichWithUserInfo(reviewMapper.toResponseDTO(saved))));
    }

    @GetMapping("/mc/{mcId}")
    public ResponseEntity<ApiResponse<List<ReviewResponseDTO>>> getMCReviews(@PathVariable String mcId) {
        List<ReviewResponseDTO> dtos = reviewService.getReviewsByMc(mcId).stream()
                .map(reviewMapper::toResponseDTO)
                .map(this::enrichWithUserInfo)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable String id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }

    private ReviewResponseDTO enrichWithUserInfo(ReviewResponseDTO dto) {
        if (dto.getClient() != null) {
            userRepository.findById(dto.getClient()).ifPresent(user -> {
                dto.setClientName(user.getName());
                dto.setClientAvatar(user.getAvatar());
            });
        }
        return dto;
    }
}
