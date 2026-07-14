package com.mchub.services.impl;

import com.mchub.models.Competition;
import com.mchub.models.CompetitionRecord;
import com.mchub.models.User;
import com.mchub.models.UserStats;
import com.mchub.repositories.CompetitionRecordRepository;
import com.mchub.repositories.CompetitionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.UserStatsRepository;
import com.mchub.services.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationServiceImpl implements GamificationService {

    private final UserStatsRepository userStatsRepository;
    private final UserRepository userRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionRecordRepository competitionRecordRepository;

    private static final double PRACTICE_SESSION_ESTIMATED_HOURS = 0.05; // 3 minutes per session

    @Override
    public UserStats getOrCreateUserStats(String userId) {
        return userStatsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserStats stats = UserStats.builder()
                            .userId(userId)
                            .currentStreak(0)
                            .longestStreak(0)
                            .totalPracticeHours(0.0)
                            .totalSessions(0)
                            .cumulativeXP(0.0)
                            .currentTier("BRONZE")
                            .weeklyXP(0.0)
                            .build();
                    return userStatsRepository.save(stats);
                });
    }

    @Override
    public UserStats processPracticeSession(String userId, String lessonId, double accuracy, double rhythm) {
        log.info("Processing gamification for user: {} with accuracy: {} and rhythm: {}", userId, accuracy, rhythm);
        
        UserStats stats = getOrCreateUserStats(userId);
        
        // 1. Calculate XP (e.g., accuracy * rhythm / 100 or simply average of both)
        double xpEarned = (accuracy + rhythm) * 0.5;
        
        // 2. Calculate Streak
        Instant now = Instant.now();
        if (stats.getLastPracticeTime() != null) {
            LocalDate lastDate = stats.getLastPracticeTime().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            long days = ChronoUnit.DAYS.between(lastDate, today);
            
            if (days == 1) {
                stats.setCurrentStreak(stats.getCurrentStreak() + 1);
            } else if (days > 1) {
                stats.setCurrentStreak(1);
            }
            // If days == 0 (practiced today already), streak stays the same
        } else {
            stats.setCurrentStreak(1);
        }
        
        stats.setLongestStreak(Math.max(stats.getLongestStreak(), stats.getCurrentStreak()));
        stats.setLastPracticeTime(now);
        
        // 3. Increment practice hours & sessions
        stats.setTotalSessions(stats.getTotalSessions() + 1);
        stats.setTotalPracticeHours(stats.getTotalPracticeHours() + PRACTICE_SESSION_ESTIMATED_HOURS);
        
        // 4. Update XP
        stats.setCumulativeXP(stats.getCumulativeXP() + xpEarned);
        stats.setWeeklyXP(stats.getWeeklyXP() + xpEarned);
        
        // 5. Update Tier
        stats.setCurrentTier(calculateTier(stats.getCumulativeXP()));
        
        UserStats savedStats = userStatsRepository.save(stats);
        
        // 6. Check and update active Competitions (Daily/Weekly Arena)
        try {
            processCompetitions(userId, lessonId, accuracy, rhythm, xpEarned);
        } catch (Exception e) {
            log.error("Failed to update competition records", e);
        }
        
        return savedStats;
    }

    @Override
    public UserStats processLoginStreak(String userId) {
        UserStats stats = getOrCreateUserStats(userId);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate last = stats.getLastLoginDate();

        // Auto-refill freeze on first login of a new month
        LocalDate lastFreeze = stats.getLastFreezeGranted();
        if (lastFreeze == null || lastFreeze.getMonth() != today.getMonth() || lastFreeze.getYear() != today.getYear()) {
            stats.setFreezesAvailable(1);
            stats.setLastFreezeGranted(today);
        }

        if (last == null) {
            stats.setLoginStreak(1);
        } else {
            long gap = ChronoUnit.DAYS.between(last, today);
            if (gap == 0) {
                // Already logged in today — no-op
                return stats;
            } else if (gap == 1) {
                stats.setLoginStreak(stats.getLoginStreak() + 1);
            } else if (gap == 2 && stats.getFreezesAvailable() > 0) {
                // Use freeze: skipped 1 day, still count as consecutive
                stats.setFreezesAvailable(stats.getFreezesAvailable() - 1);
                stats.setLoginStreak(stats.getLoginStreak() + 1);
                log.info("Freeze used for user: {} (streak kept at {})", userId, stats.getLoginStreak());
            } else {
                stats.setLoginStreak(1);
            }
        }

        stats.setLongestLoginStreak(Math.max(stats.getLongestLoginStreak(), stats.getLoginStreak()));
        stats.setLastLoginDate(today);
        return userStatsRepository.save(stats);
    }

    @Override
    public UserStats addMinigameXP(String userId, double xpEarned) {
        UserStats stats = getOrCreateUserStats(userId);
        stats.setCumulativeXP(stats.getCumulativeXP() + xpEarned);
        stats.setWeeklyXP(stats.getWeeklyXP() + xpEarned);
        stats.setCurrentTier(calculateTier(stats.getCumulativeXP()));
        return userStatsRepository.save(stats);
    }

    private String calculateTier(double cumulativeXP) {
        if (cumulativeXP >= 25000) return "ELITE_LEGEND";
        if (cumulativeXP >= 10000) return "DIAMOND";
        if (cumulativeXP >= 4000) return "PLATINUM";
        if (cumulativeXP >= 1500) return "GOLD";
        if (cumulativeXP >= 500) return "SILVER";
        return "BRONZE";
    }

    private void processCompetitions(String userId, String lessonId, double accuracy, double rhythm, double xpEarned) {
        List<Competition> activeCompetitions = competitionRepository.findByActive(true);
        if (activeCompetitions.isEmpty()) return;

        User user = userRepository.findById(userId).orElse(null);
        String userName = user != null ? user.getName() : "Anonymous MC";
        String userAvatar = user != null ? user.getAvatar() : "default-avatar.png";

        for (Competition comp : activeCompetitions) {
            // Check if this competition requires practicing the specified lesson/script
            if (comp.getChallengeScriptId() != null && comp.getChallengeScriptId().equals(lessonId)) {
                Optional<CompetitionRecord> recordOpt = competitionRecordRepository
                        .findByCompetitionIdAndUserId(comp.getId(), userId);
                
                CompetitionRecord record;
                if (recordOpt.isPresent()) {
                    record = recordOpt.get();
                    record.setAttemptCount(record.getAttemptCount() + 1);
                    record.setPracticeHours(record.getPracticeHours() + PRACTICE_SESSION_ESTIMATED_HOURS);
                    record.setBestAccuracy(Math.max(record.getBestAccuracy(), accuracy));
                    record.setBestRhythm(Math.max(record.getBestRhythm(), rhythm));
                    record.setPointsEarned(record.getPointsEarned() + xpEarned);
                    record.setLastUpdated(Instant.now());
                } else {
                    record = CompetitionRecord.builder()
                            .competitionId(comp.getId())
                            .userId(userId)
                            .userName(userName)
                            .userAvatar(userAvatar)
                            .bestAccuracy(accuracy)
                            .bestRhythm(rhythm)
                            .practiceHours(PRACTICE_SESSION_ESTIMATED_HOURS)
                            .attemptCount(1)
                            .pointsEarned(xpEarned)
                            .lastUpdated(Instant.now())
                            .build();
                }
                competitionRecordRepository.save(record);
                log.info("Saved competition record for competition: {}, user: {}", comp.getId(), userId);
            }
        }
    }
}
