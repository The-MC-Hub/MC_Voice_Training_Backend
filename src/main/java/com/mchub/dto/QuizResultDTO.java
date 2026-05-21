package com.mchub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDTO {
    private int score;           // 0–100
    private int correctCount;
    private int totalQuestions;
    private boolean passed;
    private int passingScore;
    private boolean certificateEarned;
    private String certificateId;
    private List<QuestionFeedback> feedback;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuestionFeedback {
        private int questionIndex;
        private String question;
        private int yourAnswer;      // index chosen
        private int correctAnswer;   // correct index
        private boolean correct;
        private String explanation;
    }
}
