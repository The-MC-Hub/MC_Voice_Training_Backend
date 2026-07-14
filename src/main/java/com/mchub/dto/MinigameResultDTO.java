package com.mchub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinigameResultDTO {
    private int score;
    private double xpEarned;
    private boolean isNewPersonalBest;
    private int personalBestScore;
}
