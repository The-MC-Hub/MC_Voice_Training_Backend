package com.mchub.services;

import com.mchub.models.UserStats;
import com.mchub.models.PracticeSession;

public interface GamificationService {
    UserStats getOrCreateUserStats(String userId);
    UserStats processPracticeSession(String userId, String lessonId, double accuracy, double rhythm, double overallScore, double durationSeconds);
    UserStats processLoginStreak(String userId);

    /** Adds XP from a minigame run. Does not touch practice streak/session counters. */
    UserStats addMinigameXP(String userId, double xpEarned);

    /**
     * Awards XP + a discount voucher for completing a course (quiz passed, first time only —
     * caller is responsible for only invoking this once per user+course).
     * @return the voucher code issued (or the existing one, if already issued for this course+user)
     */
    String processCourseCompletion(String userId, String courseId, int quizScore);
}
