package com.mchub.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuizSubmitRequest {
    /** Index of the selected option for each question (same order as quiz) */
    private List<Integer> answers;
}
