package com.mchub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseResultDTO {
    private boolean correct;
    private String explanation;
    private CourseResponseDTO.EnrollmentProgressDTO progress;
}
