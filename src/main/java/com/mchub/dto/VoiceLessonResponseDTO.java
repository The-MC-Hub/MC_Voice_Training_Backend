package com.mchub.dto;

import com.mchub.enums.VoiceLessonCategory;
import com.mchub.models.VoiceLesson;
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
public class VoiceLessonResponseDTO {
    private String id;
    private String title;
    private String content;
    private VoiceLessonCategory category;
    private String difficulty;
    private String description;
    private String thumbnailUrl;
    private List<VoiceLesson.EvaluationCriteria> evaluationCriteria;
    private int targetWpmMin;
    private int targetWpmMax;
    private String evaluationHint;
    private int passingScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
