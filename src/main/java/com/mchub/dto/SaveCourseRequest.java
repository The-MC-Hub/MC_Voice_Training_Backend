package com.mchub.dto;

import com.mchub.enums.CourseType;
import lombok.Data;

import java.util.List;

/** Used for both create and update by Admin */
@Data
public class SaveCourseRequest {
    private String title;
    private String shortDescription;
    private String description;
    private String slug;
    private CourseType type;
    private String thumbnail;
    private String difficulty;
    private int estimatedHours;
    private List<String> lessonIds;    // exactly 10
    private List<String> readingIds;   // exactly 3
    private List<QuizQuestionRequest> quizQuestions;
    private int passingScore;
    private boolean isActive;

    @Data
    public static class QuizQuestionRequest {
        private String question;
        private List<String> options;  // 4 options
        private int correctIndex;      // 0–3
        private String explanation;
        private String category;
    }
}
