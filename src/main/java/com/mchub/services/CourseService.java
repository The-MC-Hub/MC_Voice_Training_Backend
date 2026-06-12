package com.mchub.services;

import com.mchub.dto.*;
import com.mchub.enums.CourseType;

import java.util.List;

public interface CourseService {

    // ── Public / User ────────────────────────────────────────────────
    List<String> getAllCourseTypes();
    List<CourseResponseDTO> getAllActiveCourses();
    List<CourseResponseDTO> getCoursesByType(CourseType type);
    List<CourseResponseDTO> getMilestoneCourses(String userId);
    CourseResponseDTO getCourseDetail(String courseId, String userId);
    com.mchub.models.ReadingGuide getReadingGuide(String id);

    // ── Enrollment ───────────────────────────────────────────────────
    boolean hasCourseAccess(String courseId, String userId);
    CourseResponseDTO.EnrollmentProgressDTO enroll(String courseId, String userId);
    CourseResponseDTO.EnrollmentProgressDTO completeLesson(String courseId, String lessonId, String userId);
    CourseResponseDTO.EnrollmentProgressDTO completeReading(String courseId, String readingId, String userId);
    QuizResultDTO submitQuiz(String courseId, String userId, QuizSubmitRequest request);
    List<CourseResponseDTO> getMyEnrolledCourses(String userId);

    // ── Certificates ─────────────────────────────────────────────────
    List<CertificateResponseDTO> getMyCertificates(String userId);

    // ── Admin CRUD ───────────────────────────────────────────────────
    CourseResponseDTO createCourse(SaveCourseRequest request);
    CourseResponseDTO updateCourse(String courseId, SaveCourseRequest request);
    void deleteCourse(String courseId);
    List<CourseResponseDTO> getAllCoursesAdmin();
    CourseResponseDTO updatePricing(String courseId, Integer priceVnd, Integer discountPercent);
}
