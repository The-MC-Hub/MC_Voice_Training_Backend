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
import java.time.LocalDate;
import java.util.List;

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

    // ── Login streak (separate from practice streak) ──────────────────────────
    @Builder.Default
    private int loginStreak = 0;

    @Builder.Default
    private int longestLoginStreak = 0;

    private LocalDate lastLoginDate;

    // Freeze: skipping 1 day won't break streak. Resets to 1 on the 1st of each month.
    @Builder.Default
    private int freezesAvailable = 1;

    private LocalDate lastFreezeGranted; // month tracking for auto-refill

    // ── Practice achievements ───────────────────────────────────────────────
    /** Consecutive practice sessions scoring overallScore >= 90 (resets on any lower score) */
    @Builder.Default
    private int highScoreStreak = 0;

    @Builder.Default
    private int longestHighScoreStreak = 0;

    /** Earned badge slugs, e.g. "SESSIONS_100", "HIGH_SCORE_STREAK_5" — awarded once, never removed */
    @Builder.Default
    private List<String> earnedBadges = new java.util.ArrayList<>();
}
