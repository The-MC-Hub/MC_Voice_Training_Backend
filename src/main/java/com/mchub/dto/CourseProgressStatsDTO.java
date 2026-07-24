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
public class CourseProgressStatsDTO {
    private double totalPracticeHours;
    private int totalSessions;
    private double avgScore;
    private List<ScorePoint> scoreOverTime;
    private List<WeakLesson> weakestLessons;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScorePoint {
        private String date; // yyyy-MM-dd
        private double avgScore;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WeakLesson {
        private String lessonId;
        private String lessonTitle;
        private double avgScore;
        private int attempts;
    }
}
