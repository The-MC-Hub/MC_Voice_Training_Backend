package com.mchub.services;

import com.mchub.models.UserStats;
import com.mchub.models.PracticeSession;

public interface GamificationService {
    UserStats getOrCreateUserStats(String userId);
    UserStats processPracticeSession(String userId, String lessonId, double accuracy, double rhythm);
    UserStats processLoginStreak(String userId);
}
