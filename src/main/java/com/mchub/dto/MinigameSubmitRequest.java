package com.mchub.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MinigameSubmitRequest {
    @NotBlank
    private String difficulty; // EASY | NORMAL | HARD

    @Min(0)
    private int roundsCleared;

    @Min(0)
    private int bestCombo;
}
