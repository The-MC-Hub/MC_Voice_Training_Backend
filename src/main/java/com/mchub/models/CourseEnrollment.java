package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "course_enrollments")
@CompoundIndex(name = "user_course_idx", def = "{'userId': 1, 'courseId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEnrollment {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String courseId;

    @Builder.Default
    private List<String> completedLessonIds = new ArrayList<>();

    @Builder.Default
    private List<String> completedReadingIds = new ArrayList<>();

    private Integer quizScore;    // null = not attempted

    @Builder.Default
    private int quizAttempts = 0;

    @Builder.Default
    private double completionRate = 0.0; // 0–100

    @Builder.Default
    @Field("isCompleted")
    private boolean completed = false;

    @CreatedDate
    private LocalDateTime enrolledAt;

    private LocalDateTime completedAt;
}
