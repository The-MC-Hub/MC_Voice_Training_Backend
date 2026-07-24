package com.mchub.models;

import com.mchub.enums.CourseType;
import com.mchub.enums.ExerciseType;
import com.mchub.enums.LearningPathType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    private String id;

    private String title;

    private String shortDescription;

    private String description;

    @Indexed(unique = true)
    private String slug; // e.g. "mc-dam-cuoi", "su-kien-doanh-nghiep"

    private CourseType type;

    @Builder.Default
    private LearningPathType learningPathType = LearningPathType.STRUCTURED_COURSE;

    private String thumbnail;

    private String difficulty; // BEGINNER, INTERMEDIATE, ADVANCED

    private int estimatedHours;

    @Builder.Default
    private List<String> lessonIds = new ArrayList<>(); // 10 VoiceLesson ids

    @Builder.Default
    private List<String> readingIds = new ArrayList<>(); // 3 ReadingGuide ids

    @Builder.Default
    private List<QuizQuestion> quizQuestions = new ArrayList<>(); // 8–10 questions

    @Builder.Default
    private List<Exercise> exercises = new ArrayList<>(); // mid-course interactive exercises

    @Builder.Default
    private List<String> caseStudyIds = new ArrayList<>(); // refs to CaseStudy documents

    @Builder.Default
    private List<String> outcomes = new ArrayList<>(); // learning outcome bullet points shown on course page

    @Builder.Default
    private int passingScore = 70; // % required to pass quiz

    // ── Pricing: course included with BASIC+ plans, or buy individually ──
    @Builder.Default
    private int priceVnd = 199_000; // single-course purchase price

    @Builder.Default
    private int discountPercent = 0; // 0–100, admin-controlled

    @Builder.Default
    private boolean isActive = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ----------------------------------------------------------------
    //  Embedded quiz question
    // ----------------------------------------------------------------
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestion {
        private String question;

        @Builder.Default
        private List<String> options = new ArrayList<>(); // 4 options A/B/C/D

        private int correctIndex; // 0 = A, 1 = B, 2 = C, 3 = D

        private String explanation;

        private String category; // THEORY, PRONUNCIATION, TECHNIQUE, ETIQUETTE
    }

    // ----------------------------------------------------------------
    //  Embedded mid-course interactive exercise
    // ----------------------------------------------------------------
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Exercise {
        private String id;

        private ExerciseType type;

        private String prompt;

        /**
         * MATCHING: pairs flattened as ["left1","right1","left2","right2",...]
         * FILL_BLANK: correct words in blank order
         * SENTENCE_ORDER: sentence fragments in correct order
         */
        @Builder.Default
        private List<String> items = new ArrayList<>();

        /** Extra decoy options shown alongside the correct items (MATCHING / SENTENCE_ORDER) */
        @Builder.Default
        private List<String> distractors = new ArrayList<>();

        private String explanation;
    }
}
