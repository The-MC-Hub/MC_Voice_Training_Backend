package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "user_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStats {
    @Id
    private String id;

    @Indexed(unique = true)
    @Field("userId")
    private String userId;

    private int currentStreak;
    private int longestStreak;
    private double totalPracticeHours;
    private int totalSessions;

    private double cumulativeXP;
    private String currentTier; // BRONZE, SILVER, GOLD, PLATINUM, DIAMOND, ELITE_LEGEND
    
    private Instant lastPracticeTime;
    private double weeklyXP;
}
