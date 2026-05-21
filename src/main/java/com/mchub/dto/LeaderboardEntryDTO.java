package com.mchub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDTO {
    private String userId;
    private String userName;
    private String userAvatar;
    private double totalPracticeHours;
    private int totalSessions;
    private double cumulativeXP;
    private int currentStreak;
    private String currentTier;
}
