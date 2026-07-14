package com.mchub.services;

import com.mchub.models.UserStats;
import com.mchub.models.PracticeSession;

public interface GamificationService {
    UserStats getOrCreateUserStats(String userId);
    UserStats processPracticeSession(String userId, String lessonId, double accuracy, double rhythm);
    UserStats processLoginStreak(String userId);

    /** Adds XP from a minigame run. Does not touch practice streak/session counters. */
    UserStats addMinigameXP(String userId, double xpEarned);
}
