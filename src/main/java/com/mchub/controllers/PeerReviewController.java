package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.PracticeReviewDTO;
import com.mchub.dto.SubmitReviewRequest;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.PracticeReview;
import com.mchub.models.PracticeSession;
import com.mchub.models.User;
import com.mchub.models.VoiceLesson;
import com.mchub.repositories.PracticeReviewRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.VoiceLessonRepository;
import com.mchub.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/peer-review")
@RequiredArgsConstructor
public class PeerReviewController {

    private final PracticeReviewRepository practiceReviewRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final VoiceLessonRepository lessonRepository;
    private final UserRepository userRepository;

    /** POST /api/v1/peer-review/request/{practiceSessionId} — learner opts in to have an MC review their practice */
    @PostMapping("/request/{practiceSessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PracticeReviewDTO>> requestReview(@PathVariable String practiceSessionId) {
        String userId = SecurityUtils.getCurrentUserId();
        PracticeSession session = practiceSessionRepository.findById(practiceSessionId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Practice session not found: " + practiceSessionId));
        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You do not own this practice session");
        }
        if (practiceReviewRepository.existsByPracticeSessionId(practiceSessionId)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Review already requested for this session");
        }
        PracticeReview review = PracticeReview.builder()
                .practiceSessionId(practiceSessionId)
                .revieweeId(userId)
                .status("PENDING")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Review requested",
                toDTO(practiceReviewRepository.save(review))));
    }

    /** GET /api/v1/peer-review/pending — MC-only: list sessions awaiting review */
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('MC')")
    public ResponseEntity<ApiResponse<List<PracticeReviewDTO>>> pending() {
        List<PracticeReview> reviews = practiceReviewRepository.findByStatus("PENDING");
        return ResponseEntity.ok(ApiResponse.success("Pending reviews retrieved",
                reviews.stream().map(this::toDTO).collect(Collectors.toList())));
    }

    /** POST /api/v1/peer-review/{id}/submit — MC submits a comment + rating, claiming the review */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('MC')")
    public ResponseEntity<ApiResponse<PracticeReviewDTO>> submit(
            @PathVariable String id, @Valid @RequestBody SubmitReviewRequest request) {
        String reviewerId = SecurityUtils.getCurrentUserId();
        PracticeReview review = practiceReviewRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Review not found: " + id));
        if (!"PENDING".equals(review.getStatus())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Review already completed");
        }
        review.setReviewerId(reviewerId);
        review.setComment(request.getComment());
        review.setRating(request.getRating());
        review.setStatus("REVIEWED");
        review.setReviewedAt(LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.success("Review submitted",
                toDTO(practiceReviewRepository.save(review))));
    }

    /** GET /api/v1/peer-review/my — learner's own review requests (any status) */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PracticeReviewDTO>>> myReviews() {
        String userId = SecurityUtils.getCurrentUserId();
        List<PracticeReview> reviews = practiceReviewRepository.findByRevieweeId(userId);
        return ResponseEntity.ok(ApiResponse.success("My reviews retrieved",
                reviews.stream().map(this::toDTO).collect(Collectors.toList())));
    }

    /** GET /api/v1/peer-review/session/{practiceSessionId} — review for a specific session, if any */
    @GetMapping("/session/{practiceSessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PracticeReviewDTO>> forSession(@PathVariable String practiceSessionId) {
        PracticeReview review = practiceReviewRepository.findByPracticeSessionId(practiceSessionId).orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Review retrieved", review != null ? toDTO(review) : null));
    }

    private PracticeReviewDTO toDTO(PracticeReview r) {
        PracticeSession session = practiceSessionRepository.findById(r.getPracticeSessionId()).orElse(null);
        VoiceLesson lesson = session != null ? lessonRepository.findById(session.getLessonId()).orElse(null) : null;
        User reviewee = userRepository.findById(r.getRevieweeId()).orElse(null);
        User reviewer = r.getReviewerId() != null ? userRepository.findById(r.getReviewerId()).orElse(null) : null;

        return PracticeReviewDTO.builder()
                .id(r.getId())
                .practiceSessionId(r.getPracticeSessionId())
                .lessonId(session != null ? session.getLessonId() : null)
                .lessonTitle(lesson != null ? lesson.getTitle() : null)
                .revieweeId(r.getRevieweeId())
                .revieweeName(reviewee != null ? reviewee.getName() : null)
                .reviewerId(r.getReviewerId())
                .reviewerName(reviewer != null ? reviewer.getName() : null)
                .comment(r.getComment())
                .rating(r.getRating())
                .status(r.getStatus())
                .audioUrl(session != null ? session.getAudioUrl() : null)
                .createdAt(r.getCreatedAt())
                .reviewedAt(r.getReviewedAt())
                .build();
    }
}
