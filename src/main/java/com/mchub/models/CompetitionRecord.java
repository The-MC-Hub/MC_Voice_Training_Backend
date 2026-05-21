package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "competition_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionRecord {
    @Id
    private String id;
    
    private String competitionId;
    private String userId;
    private String userName;
    private String userAvatar;
    
    private double bestAccuracy;
    private double bestRhythm;
    private double practiceHours;
    private int attemptCount;
    
    private double pointsEarned; // XP gained in this competition
    private Instant lastUpdated;
}
