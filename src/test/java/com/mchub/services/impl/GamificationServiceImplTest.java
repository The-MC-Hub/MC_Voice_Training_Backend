package com.mchub.services.impl;

import com.mchub.models.Competition;
import com.mchub.models.CompetitionRecord;
import com.mchub.models.User;
import com.mchub.models.UserStats;
import com.mchub.repositories.CompetitionRecordRepository;
import com.mchub.repositories.CompetitionRepository;
import com.mchub.repositories.DiscountCodeRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.UserStatsRepository;
import com.mchub.repositories.UserVoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GamificationServiceImpl. Mocks all repositories — no real DB.
 * Covers XP/tier calculation, practice-streak and login-streak logic (including
 * the freeze mechanic), badge idempotency, and the fire-and-forget competition
 * update path.
 */
@ExtendWith(MockitoExtension.class)
class GamificationServiceImplTest {

    @Mock private UserStatsRepository userStatsRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionRecordRepository competitionRecordRepository;
    @Mock private DiscountCodeRepository discountCodeRepository;
    @Mock private UserVoucherRepository userVoucherRepository;

    private GamificationServiceImpl service;

    private static final String USER_ID = "user-gamify-001";

    @BeforeEach
    void setUp() {
        service = new GamificationServiceImpl(
                userStatsRepository, userRepository, competitionRepository, competitionRecordRepository,
                discountCodeRepository, userVoucherRepository);
        // Most tests don't exercise the competition path — default to no active competitions.
        org.mockito.Mockito.lenient().when(competitionRepository.findByActive(true)).thenReturn(List.of());
    }

    private UserStats.UserStatsBuilder baseStats() {
        return UserStats.builder().userId(USER_ID).currentStreak(0).longestStreak(0)
                .totalPracticeHours(0.0).totalSessions(0).cumulativeXP(0.0).currentTier("BRONZE")
                .weeklyXP(0.0).earnedBadges(new java.util.ArrayList<>());
    }

    @Nested
    @DisplayName("getOrCreateUserStats")
    class GetOrCreate {

        @Test
        @DisplayName("creates fresh BRONZE-tier stats when none exist")
        void createsFreshStats() {
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            UserStats result = service.getOrCreateUserStats(USER_ID);

            assertThat(result.getCurrentTier()).isEqualTo("BRONZE");
            assertThat(result.getCumulativeXP()).isZero();
        }

        @Test
        @DisplayName("returns existing stats without creating a new one")
        void returnsExistingStats() {
            UserStats existing = baseStats().cumulativeXP(1000).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

            UserStats result = service.getOrCreateUserStats(USER_ID);

            assertThat(result).isSameAs(existing);
            verify(userStatsRepository, never()).save(any(UserStats.class));
        }
    }

    @Nested
    @DisplayName("processPracticeSession — XP and tier")
    class XpAndTier {

        @Test
        @DisplayName("XP earned is the average of accuracy and rhythm")
        void xpIsAverageOfAccuracyAndRhythm() {
            UserStats stats = baseStats().build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 80.0, 60.0, 75.0, 0.0);

            assertThat(stats.getCumulativeXP()).isEqualTo(70.0); // (80+60)*0.5
            assertThat(stats.getWeeklyXP()).isEqualTo(70.0);
        }

        @Test
        @DisplayName("tier upgrades to SILVER at 500 XP")
        void upgradesToSilverAt500() {
            UserStats stats = baseStats().cumulativeXP(450).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 80.0, 80.0, 75.0, 0.0); // +80 XP -> 530

            assertThat(stats.getCurrentTier()).isEqualTo("SILVER");
        }

        @Test
        @DisplayName("tier is ELITE_LEGEND at 25000+ XP")
        void eliteLegendAt25000() {
            UserStats stats = baseStats().cumulativeXP(25000).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 0.0, 0.0, 0.0, 0.0);

            assertThat(stats.getCurrentTier()).isEqualTo("ELITE_LEGEND");
        }
    }

    @Nested
    @DisplayName("processPracticeSession — practice streak")
    class PracticeStreak {

        @Test
        @DisplayName("first-ever session sets streak to 1")
        void firstSessionSetsStreakToOne() {
            UserStats stats = baseStats().lastPracticeTime(null).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 50.0, 50.0, 50.0, 0.0);

            assertThat(stats.getCurrentStreak()).isEqualTo(1);
        }

        @Test
        @DisplayName("practicing again the same day does not change the streak")
        void sameDayDoesNotChangeStreak() {
            UserStats stats = baseStats().currentStreak(3).lastPracticeTime(Instant.now()).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 50.0, 50.0, 50.0, 0.0);

            assertThat(stats.getCurrentStreak()).isEqualTo(3);
        }

        @Test
        @DisplayName("practicing exactly 1 day after last session increments the streak")
        void oneDayGapIncrementsStreak() {
            Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
            UserStats stats = baseStats().currentStreak(3).lastPracticeTime(yesterday).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 50.0, 50.0, 50.0, 0.0);

            assertThat(stats.getCurrentStreak()).isEqualTo(4);
        }

        @Test
        @DisplayName("gap of more than 1 day resets streak to 1")
        void multiDayGapResetsStreak() {
            Instant threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS);
            UserStats stats = baseStats().currentStreak(10).lastPracticeTime(threeDaysAgo).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 50.0, 50.0, 50.0, 0.0);

            assertThat(stats.getCurrentStreak()).isEqualTo(1);
        }

        @Test
        @DisplayName("longestStreak tracks the maximum ever reached, not reset with currentStreak")
        void longestStreakTracksMax() {
            Instant threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS);
            UserStats stats = baseStats().currentStreak(10).longestStreak(10).lastPracticeTime(threeDaysAgo).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 50.0, 50.0, 50.0, 0.0); // resets currentStreak to 1

            assertThat(stats.getCurrentStreak()).isEqualTo(1);
            assertThat(stats.getLongestStreak()).isEqualTo(10); // preserved
        }
    }

    @Nested
    @DisplayName("processPracticeSession — high score streak")
    class HighScoreStreak {

        @Test
        @DisplayName("increments highScoreStreak when overallScore >= 90")
        void incrementsOnHighScore() {
            UserStats stats = baseStats().highScoreStreak(2).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 50.0, 50.0, 92.0, 0.0);

            assertThat(stats.getHighScoreStreak()).isEqualTo(3);
        }

        @Test
        @DisplayName("resets highScoreStreak to 0 when overallScore < 90")
        void resetsOnLowScore() {
            UserStats stats = baseStats().highScoreStreak(4).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 50.0, 50.0, 89.9, 0.0);

            assertThat(stats.getHighScoreStreak()).isZero();
        }
    }

    @Nested
    @DisplayName("processPracticeSession — badge idempotency")
    class BadgeIdempotency {

        @Test
        @DisplayName("awards SESSIONS_10 exactly once even across multiple qualifying sessions")
        void awardsSessions10Once() {
            UserStats stats = baseStats().totalSessions(9).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 50.0, 50.0, 50.0, 0.0); // totalSessions -> 10
            assertThat(stats.getEarnedBadges()).containsExactlyInAnyOrder("SESSIONS_10");

            service.processPracticeSession(USER_ID, "lesson-1", 50.0, 50.0, 50.0, 0.0); // totalSessions -> 11
            assertThat(stats.getEarnedBadges()).containsExactlyInAnyOrder("SESSIONS_10"); // not duplicated
        }

        @Test
        @DisplayName("does not award STREAK_7 badge below the threshold")
        void doesNotAwardBelowThreshold() {
            UserStats stats = baseStats().currentStreak(0).lastPracticeTime(null).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 50.0, 50.0, 50.0, 0.0); // streak becomes 1

            assertThat(stats.getEarnedBadges()).doesNotContain("STREAK_7");
        }
    }

    @Nested
    @DisplayName("processLoginStreak")
    class LoginStreak {

        @Test
        @DisplayName("first login sets loginStreak to 1")
        void firstLoginSetsStreakToOne() {
            UserStats stats = baseStats().lastLoginDate(null).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processLoginStreak(USER_ID);

            assertThat(stats.getLoginStreak()).isEqualTo(1);
        }

        @Test
        @DisplayName("same-day login is a no-op — does not re-save or change streak")
        void sameDayLoginIsNoOp() {
            UserStats stats = baseStats().loginStreak(5)
                    .lastLoginDate(LocalDate.now(ZoneId.systemDefault())).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));

            UserStats result = service.processLoginStreak(USER_ID);

            assertThat(result.getLoginStreak()).isEqualTo(5);
            verify(userStatsRepository, never()).save(any(UserStats.class));
        }

        @Test
        @DisplayName("1-day gap increments loginStreak")
        void oneDayGapIncrementsStreak() {
            UserStats stats = baseStats().loginStreak(3)
                    .lastLoginDate(LocalDate.now(ZoneId.systemDefault()).minusDays(1))
                    .freezesAvailable(1).lastFreezeGranted(LocalDate.now(ZoneId.systemDefault())).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processLoginStreak(USER_ID);

            assertThat(stats.getLoginStreak()).isEqualTo(4);
        }

        @Test
        @DisplayName("2-day gap with a freeze available consumes the freeze and keeps streak alive")
        void twoDayGapWithFreezeKeepsStreak() {
            UserStats stats = baseStats().loginStreak(5)
                    .lastLoginDate(LocalDate.now(ZoneId.systemDefault()).minusDays(2))
                    .freezesAvailable(1).lastFreezeGranted(LocalDate.now(ZoneId.systemDefault())).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processLoginStreak(USER_ID);

            assertThat(stats.getLoginStreak()).isEqualTo(6); // preserved + incremented
            assertThat(stats.getFreezesAvailable()).isZero(); // freeze consumed
        }

        @Test
        @DisplayName("2-day gap WITHOUT a freeze resets streak to 1")
        void twoDayGapWithoutFreezeResets() {
            UserStats stats = baseStats().loginStreak(5)
                    .lastLoginDate(LocalDate.now(ZoneId.systemDefault()).minusDays(2))
                    .freezesAvailable(0).lastFreezeGranted(LocalDate.now(ZoneId.systemDefault())).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processLoginStreak(USER_ID);

            assertThat(stats.getLoginStreak()).isEqualTo(1);
        }

        @Test
        @DisplayName("gap of 3+ days resets streak to 1 regardless of freeze availability")
        void threeDayGapAlwaysResets() {
            UserStats stats = baseStats().loginStreak(5)
                    .lastLoginDate(LocalDate.now(ZoneId.systemDefault()).minusDays(3))
                    .freezesAvailable(1).lastFreezeGranted(LocalDate.now(ZoneId.systemDefault())).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processLoginStreak(USER_ID);

            assertThat(stats.getLoginStreak()).isEqualTo(1);
            assertThat(stats.getFreezesAvailable()).isEqualTo(1); // freeze NOT consumed for 3+ day gaps
        }

        @Test
        @DisplayName("auto-refills freeze to 1 on first login of a new month")
        void refillsFreezeOnNewMonth() {
            LocalDate lastMonth = LocalDate.now(ZoneId.systemDefault()).minusMonths(1);
            UserStats stats = baseStats().loginStreak(1)
                    .lastLoginDate(lastMonth)
                    .freezesAvailable(0).lastFreezeGranted(lastMonth).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processLoginStreak(USER_ID);

            assertThat(stats.getFreezesAvailable()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("addMinigameXP")
    class AddMinigameXp {

        @Test
        @DisplayName("adds XP and recalculates tier without touching streaks/sessions")
        void addsXpWithoutTouchingStreaks() {
            UserStats stats = baseStats().cumulativeXP(0).currentStreak(5).totalSessions(20).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            service.addMinigameXP(USER_ID, 600.0);

            assertThat(stats.getCumulativeXP()).isEqualTo(600.0);
            assertThat(stats.getCurrentTier()).isEqualTo("SILVER");
            assertThat(stats.getCurrentStreak()).isEqualTo(5); // untouched
            assertThat(stats.getTotalSessions()).isEqualTo(20); // untouched
        }
    }

    @Nested
    @DisplayName("processCompetitions (via processPracticeSession fire-and-forget)")
    class Competitions {

        @Test
        @DisplayName("creates a new CompetitionRecord on first practice of a matching challenge script")
        void createsNewRecordOnFirstAttempt() {
            UserStats stats = baseStats().build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            Competition comp = Competition.builder().id("comp-1").active(true).challengeScriptId("lesson-1").build();
            when(competitionRepository.findByActive(true)).thenReturn(List.of(comp));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).name("QA Tester").build()));
            when(competitionRecordRepository.findByCompetitionIdAndUserId("comp-1", USER_ID)).thenReturn(Optional.empty());
            when(competitionRecordRepository.save(any(CompetitionRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            service.processPracticeSession(USER_ID, "lesson-1", 80.0, 70.0, 75.0, 0.0);

            verify(competitionRecordRepository).save(any(CompetitionRecord.class));
        }

        @Test
        @DisplayName("does not create a record for a competition whose challengeScriptId does not match")
        void skipsNonMatchingCompetition() {
            UserStats stats = baseStats().build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

            Competition comp = Competition.builder().id("comp-1").active(true).challengeScriptId("other-lesson").build();
            when(competitionRepository.findByActive(true)).thenReturn(List.of(comp));

            service.processPracticeSession(USER_ID, "lesson-1", 80.0, 70.0, 75.0, 0.0);

            verify(competitionRecordRepository, never()).save(any(CompetitionRecord.class));
        }

        @Test
        @DisplayName("gamification stats are still saved even if competition processing throws")
        void statsSavedEvenIfCompetitionProcessingFails() {
            UserStats stats = baseStats().build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));
            when(competitionRepository.findByActive(true)).thenThrow(new RuntimeException("DB down"));

            UserStats result = service.processPracticeSession(USER_ID, "lesson-1", 80.0, 70.0, 75.0, 0.0);

            assertThat(result.getCumulativeXP()).isEqualTo(75.0);
            verify(userStatsRepository).save(stats);
        }
    }

    @Nested
    @DisplayName("processCourseCompletion")
    class ProcessCourseCompletion {

        private static final String COURSE_ID = "course-1";

        @Test
        @DisplayName("awards 200 XP and recalculates tier")
        void awardsXpAndTier() {
            UserStats stats = baseStats().cumulativeXP(400.0).weeklyXP(0.0).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));
            when(discountCodeRepository.findByCodeIgnoreCase(any())).thenReturn(Optional.empty());
            when(userVoucherRepository.existsByUserIdAndCode(any(), any())).thenReturn(false);

            service.processCourseCompletion(USER_ID, COURSE_ID, 90);

            assertThat(stats.getCumulativeXP()).isEqualTo(600.0);
            assertThat(stats.getWeeklyXP()).isEqualTo(200.0);
            assertThat(stats.getCurrentTier()).isEqualTo("SILVER");
        }

        @Test
        @DisplayName("awards COURSE_GRADUATE badge idempotently")
        void awardsGraduateBadgeOnce() {
            UserStats stats = baseStats().earnedBadges(new java.util.ArrayList<>(List.of("COURSE_GRADUATE"))).build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));
            when(discountCodeRepository.findByCodeIgnoreCase(any())).thenReturn(Optional.empty());
            when(userVoucherRepository.existsByUserIdAndCode(any(), any())).thenReturn(false);

            service.processCourseCompletion(USER_ID, COURSE_ID, 90);

            assertThat(stats.getEarnedBadges()).containsOnlyOnce("COURSE_GRADUATE");
        }

        @Test
        @DisplayName("creates a new DiscountCode + UserVoucher when none exists yet")
        void createsVoucherWhenNotExisting() {
            UserStats stats = baseStats().build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));
            when(discountCodeRepository.findByCodeIgnoreCase(any())).thenReturn(Optional.empty());
            when(userVoucherRepository.existsByUserIdAndCode(any(), any())).thenReturn(false);

            String code = service.processCourseCompletion(USER_ID, COURSE_ID, 90);

            assertThat(code).startsWith("COURSE30-");
            verify(discountCodeRepository).save(any(com.mchub.models.DiscountCode.class));
            verify(userVoucherRepository).save(any(com.mchub.models.UserVoucher.class));
        }

        @Test
        @DisplayName("does not duplicate DiscountCode/UserVoucher when already issued (retry-safe)")
        void doesNotDuplicateExistingVoucher() {
            UserStats stats = baseStats().build();
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
            when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));
            when(discountCodeRepository.findByCodeIgnoreCase(any()))
                    .thenReturn(Optional.of(com.mchub.models.DiscountCode.builder().id("existing").build()));
            when(userVoucherRepository.existsByUserIdAndCode(any(), any())).thenReturn(true);

            service.processCourseCompletion(USER_ID, COURSE_ID, 90);

            verify(discountCodeRepository, never()).save(any(com.mchub.models.DiscountCode.class));
            verify(userVoucherRepository, never()).save(any(com.mchub.models.UserVoucher.class));
        }
    }
}
