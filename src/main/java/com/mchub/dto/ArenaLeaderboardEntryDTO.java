package com.mchub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaLeaderboardEntryDTO {
    private String userId;
    private String userName;
    private String userAvatar;
    private double bestAccuracy;
    private double bestRhythm;
    private int attemptCount;
    private double pointsEarned;
}
