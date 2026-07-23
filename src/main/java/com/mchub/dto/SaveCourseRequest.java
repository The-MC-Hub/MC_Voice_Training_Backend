package com.mchub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mchub.enums.CourseType;
import com.mchub.enums.ExerciseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** Used for both create and update by Admin */
@Data
public class SaveCourseRequest {
    @NotBlank
    private String title;
    private String shortDescription;
    private String description;
    @NotBlank
    private String slug;
    @NotNull
    private CourseType type;
    private String thumbnail;
    private String difficulty;
    private int estimatedHours;
    private List<String> lessonIds;    // exactly 10
    private List<String> readingIds;   // exactly 3
    private List<QuizQuestionRequest> quizQuestions;
    private List<ExerciseRequest> exercises;
    private List<String> caseStudyIds;
    private List<String> outcomes;
    private int passingScore;
    @JsonProperty("isActive")
    private boolean isActive;

    @Data
    public static class QuizQuestionRequest {
        @NotBlank
        private String question;
        private List<String> options;  // 4 options
        private int correctIndex;      // 0–3
        private String explanation;
        private String category;
    }

    @Data
    public static class ExerciseRequest {
        private String id;
        @NotNull
        private ExerciseType type;
        private String prompt;
        private List<String> items;
        private List<String> distractors;
        private String explanation;
    }
}
