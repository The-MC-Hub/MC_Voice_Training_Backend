package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.enums.CourseType;
import com.mchub.services.CourseService;
import com.mchub.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // ── Public ────────────────────────────────────────────────────────

    /** GET /api/v1/courses/roadmap — milestone courses ordered BEGINNER→INTERMEDIATE→ADVANCED */
    @GetMapping("/roadmap")
    public ResponseEntity<ApiResponse<List<CourseResponseDTO>>> getRoadmap() {
        String userId = tryGetUserId();
        return ResponseEntity.ok(ApiResponse.success("Roadmap retrieved",
                courseService.getMilestoneCourses(userId)));
    }

    /** GET /api/v1/courses/types — available CourseType values */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<String>>> getCourseTypes() {
        return ResponseEntity.ok(ApiResponse.success("Course types retrieved",
                courseService.getAllCourseTypes()));
    }

    /** GET /api/v1/courses — list all active courses */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponseDTO>>> listCourses(
            @RequestParam(required = false) CourseType type) {
        String userId = tryGetUserId();
        List<CourseResponseDTO> result = type != null
                ? courseService.getCoursesByType(type, userId)
                : courseService.getAllActiveCourses(userId);
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> getCourse(@PathVariable String id) {
        String userId = tryGetUserId();
        return ResponseEntity.ok(ApiResponse.success("Course detail retrieved",
                courseService.getCourseDetail(id, userId)));
    }

    /** GET /api/v1/courses/reading-guides/{id} — get a specific reading guide */
    @GetMapping("/reading-guides/{id}")
    public ResponseEntity<ApiResponse<com.mchub.models.ReadingGuide>> getReadingGuide(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Reading guide retrieved",
                courseService.getReadingGuide(id)));
    }

    // ── Authenticated user ────────────────────────────────────────────

    /** POST /api/v1/courses/{id}/enroll */
    @PostMapping("/{id}/enroll")
    public ResponseEntity<ApiResponse<CourseResponseDTO.EnrollmentProgressDTO>> enroll(
            @PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Enrolled successfully",
                courseService.enroll(id, userId)));
    }

    /** POST /api/v1/courses/{id}/gift-enroll — bypass plan check (used for welcome gift) */
    @PostMapping("/{id}/gift-enroll")
    public ResponseEntity<ApiResponse<CourseResponseDTO.EnrollmentProgressDTO>> giftEnroll(
            @PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Gift course enrolled",
                courseService.giftEnroll(id, userId)));
    }

    /** POST /api/v1/courses/{id}/lessons/{lessonId}/complete */
    @PostMapping("/{id}/lessons/{lessonId}/complete")
    public ResponseEntity<ApiResponse<CourseResponseDTO.EnrollmentProgressDTO>> completeLesson(
            @PathVariable String id,
            @PathVariable String lessonId) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Lesson marked complete",
                courseService.completeLesson(id, lessonId, userId)));
    }

    /** POST /api/v1/courses/{id}/readings/{readingId}/complete */
    @PostMapping("/{id}/readings/{readingId}/complete")
    public ResponseEntity<ApiResponse<CourseResponseDTO.EnrollmentProgressDTO>> completeReading(
            @PathVariable String id,
            @PathVariable String readingId) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Reading marked complete",
                courseService.completeReading(id, readingId, userId)));
    }

    /** POST /api/v1/courses/{id}/quiz/submit */
    @PostMapping("/{id}/quiz/submit")
    public ResponseEntity<ApiResponse<QuizResultDTO>> submitQuiz(
            @PathVariable String id,
            @Valid @RequestBody QuizSubmitRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Quiz submitted",
                courseService.submitQuiz(id, userId, request)));
    }

    /** GET /api/v1/courses/my/enrolled */
    @GetMapping("/my/enrolled")
    public ResponseEntity<ApiResponse<List<CourseResponseDTO>>> myEnrolled() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Enrolled courses retrieved",
                courseService.getMyEnrolledCourses(userId)));
    }

    /** GET /api/v1/courses/my/certificates */
    @GetMapping("/my/certificates")
    public ResponseEntity<ApiResponse<List<CertificateResponseDTO>>> myCertificates() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Certificates retrieved",
                courseService.getMyCertificates(userId)));
    }

    // ── Helper ────────────────────────────────────────────────────────

    /** Returns userId without throwing if not authenticated (for public detail endpoint) */
    private String tryGetUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
