package com.mchub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityStatsDTO {
    private long totalUsers;
    private double totalPracticeHours;
    private String mostPopularScriptTitle;
    private int activeCompetitionsCount;
}
