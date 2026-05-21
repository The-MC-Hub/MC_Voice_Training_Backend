package com.mchub.models;

import com.mchub.enums.CourseType;
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
    private int passingScore = 70; // % required to pass quiz

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
}
