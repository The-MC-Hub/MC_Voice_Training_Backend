package com.mchub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinigameLeaderboardEntryDTO {
    private int rank;
    private String userId;
    private String userName;
    private String avatar;
    private int bestScore;
    private int bestCombo;
}
