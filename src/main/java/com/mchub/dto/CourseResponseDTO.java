package com.mchub.dto;

import com.mchub.enums.CourseType;
import com.mchub.enums.LearningPathType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponseDTO {
    private String id;
    private String title;
    private String shortDescription;
    private String description;
    private String slug;
    private CourseType type;
    private LearningPathType learningPathType;
    /** Computed per-user: "Completed" | "In Progress" | "Locked" */
    private String status;
    private String thumbnail;
    private String difficulty;
    private int estimatedHours;
    private int totalLessons;
    private int totalReadings;
    private int totalQuizQuestions;
    private int passingScore;
    private boolean isActive;
    private LocalDateTime createdAt;

    // Pricing — single-course purchase (also included with BASIC+ plans)
    private int priceVnd;
    private int discountPercent;
    private int finalPriceVnd;          // priceVnd after discountPercent
    private Boolean hasAccess;          // per-user: plan BASIC+ active or purchased; null when unauthenticated
    private Boolean purchased;          // per-user: bought individually

    // Populated only on detail endpoint
    private List<VoiceLessonResponseDTO> lessons;
    private List<ReadingGuideDTO> readings;
    private List<QuizQuestionDTO> quizQuestions;

    // User-specific (null when not authenticated / not enrolled)
    private EnrollmentProgressDTO myProgress;

    // Admin stats
    private Long totalEnrollments;
    private Long totalCompletions;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReadingGuideDTO {
        private String id;
        private String title;
        private String category;
        private String thumbnail;
        private String author;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuizQuestionDTO {
        private String question;
        private List<String> options;
        private String category;
        // correctIndex intentionally omitted from public DTO
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EnrollmentProgressDTO {
        private String enrollmentId;
        private List<String> completedLessonIds;
        private List<String> completedReadingIds;
        private Integer quizScore;
        private int quizAttempts;
        private double completionRate;
        private boolean isCompleted;
        private LocalDateTime enrolledAt;
        private LocalDateTime completedAt;
    }
}
