package com.mchub.services.impl;

import com.mchub.models.Competition;
import com.mchub.models.CompetitionRecord;
import com.mchub.models.DiscountCode;
import com.mchub.models.User;
import com.mchub.models.UserStats;
import com.mchub.models.UserVoucher;
import com.mchub.repositories.CompetitionRecordRepository;
import com.mchub.repositories.CompetitionRepository;
import com.mchub.repositories.DiscountCodeRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.UserStatsRepository;
import com.mchub.repositories.UserVoucherRepository;
import com.mchub.services.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final DiscountCodeRepository discountCodeRepository;
    private final UserVoucherRepository userVoucherRepository;

    private static final double COURSE_COMPLETION_XP = 200.0;

    private static final double PRACTICE_SESSION_ESTIMATED_HOURS = 0.05; // 3 minutes per session — fallback only when AI didn't report a duration

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
    public UserStats processPracticeSession(String userId, String lessonId, double accuracy, double rhythm, double overallScore, double durationSeconds) {
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
        
        // 3. Increment practice hours & sessions — use AI-reported audio duration when available,
        // fall back to the flat estimate only if the AI service didn't report one (e.g. old sessions, proxy path).
        double hoursToAdd = durationSeconds > 0 ? durationSeconds / 3600.0 : PRACTICE_SESSION_ESTIMATED_HOURS;
        stats.setTotalSessions(stats.getTotalSessions() + 1);
        stats.setTotalPracticeHours(stats.getTotalPracticeHours() + hoursToAdd);
        
        // 4. Update XP
        stats.setCumulativeXP(stats.getCumulativeXP() + xpEarned);
        stats.setWeeklyXP(stats.getWeeklyXP() + xpEarned);
        
        // 5. Update Tier
        stats.setCurrentTier(calculateTier(stats.getCumulativeXP()));

        // 5b. High-score streak: consecutive sessions scoring >= 90 overall
        if (overallScore >= 90.0) {
            stats.setHighScoreStreak(stats.getHighScoreStreak() + 1);
            stats.setLongestHighScoreStreak(Math.max(stats.getLongestHighScoreStreak(), stats.getHighScoreStreak()));
        } else {
            stats.setHighScoreStreak(0);
        }

        // 5c. Award milestone badges (idempotent — only added once, never removed)
        awardBadgeIfEligible(stats, "SESSIONS_10", stats.getTotalSessions() >= 10);
        awardBadgeIfEligible(stats, "SESSIONS_50", stats.getTotalSessions() >= 50);
        awardBadgeIfEligible(stats, "SESSIONS_100", stats.getTotalSessions() >= 100);
        awardBadgeIfEligible(stats, "STREAK_7", stats.getCurrentStreak() >= 7);
        awardBadgeIfEligible(stats, "STREAK_30", stats.getCurrentStreak() >= 30);
        awardBadgeIfEligible(stats, "HIGH_SCORE_STREAK_5", stats.getHighScoreStreak() >= 5);
        awardBadgeIfEligible(stats, "HIGH_SCORE_STREAK_10", stats.getHighScoreStreak() >= 10);

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

    @Override
    public String processCourseCompletion(String userId, String courseId, int quizScore) {
        UserStats stats = getOrCreateUserStats(userId);

        stats.setCumulativeXP(stats.getCumulativeXP() + COURSE_COMPLETION_XP);
        stats.setWeeklyXP(stats.getWeeklyXP() + COURSE_COMPLETION_XP);
        stats.setCurrentTier(calculateTier(stats.getCumulativeXP()));
        awardBadgeIfEligible(stats, "COURSE_GRADUATE", true);
        userStatsRepository.save(stats);

        return issueCourseCompletionVoucher(userId, courseId);
    }

    private String issueCourseCompletionVoucher(String userId, String courseId) {
        String code = "COURSE30-" + courseId.substring(Math.max(0, courseId.length() - 6)).toUpperCase()
                + "-" + userId.substring(Math.max(0, userId.length() - 6)).toUpperCase();

        DiscountCode existing = discountCodeRepository.findByCodeIgnoreCase(code).orElse(null);
        if (existing == null) {
            DiscountCode voucher = DiscountCode.builder()
                    .code(code)
                    .type(DiscountCode.DiscountType.PERCENT)
                    .discountValue(30)
                    .applicablePlans(List.of(com.mchub.enums.SubscriptionPlan.BASIC))
                    .maxUses(1)
                    .usedCount(0)
                    .active(true)
                    .showInSidebar(false)
                    .description("Course completion voucher for user " + userId)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();
            discountCodeRepository.save(voucher);
        }

        if (!userVoucherRepository.existsByUserIdAndCode(userId, code)) {
            UserVoucher userVoucher = UserVoucher.builder()
                    .userId(userId)
                    .code(code)
                    .discountPercent(30)
                    .description("Phần thưởng hoàn thành khóa học")
                    .source("COURSE_COMPLETION")
                    .active(true)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();
            userVoucherRepository.save(userVoucher);
        }

        return code;
    }

    private void awardBadgeIfEligible(UserStats stats, String badgeSlug, boolean eligible) {
        if (eligible && !stats.getEarnedBadges().contains(badgeSlug)) {
            stats.getEarnedBadges().add(badgeSlug);
            log.info("Badge awarded: {} to user {}", badgeSlug, stats.getUserId());
        }
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
