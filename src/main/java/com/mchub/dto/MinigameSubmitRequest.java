package com.mchub.dto;

import lombok.Data;

@Data
public class MinigameSubmitRequest {
    private String difficulty; // EASY | NORMAL | HARD
    private int roundsCleared;
    private int bestCombo;
}
