package com.mchub.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MCTrainingStatsDTO {
    private String mcId;
    private String userId;
    private String name;
    private String avatar;
    private double totalPracticeHours;
    private int scriptsCompleted;
    private double avgAccuracy;
    private double avgRhythm;
    private int totalSessions;
}
