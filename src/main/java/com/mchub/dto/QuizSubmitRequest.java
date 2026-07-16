package com.mchub.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class QuizSubmitRequest {
    /** Index of the selected option for each question (same order as quiz) */
    @NotEmpty
    private List<Integer> answers;
}
