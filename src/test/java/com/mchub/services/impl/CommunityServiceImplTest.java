package com.mchub.services.impl;

import com.mchub.dto.ActiveArenaResponseDTO;
import com.mchub.dto.CommunityStatsDTO;
import com.mchub.dto.LeaderboardEntryDTO;
import com.mchub.enums.CompetitionType;
import com.mchub.models.Competition;
import com.mchub.models.CompetitionRecord;
import com.mchub.models.UserStats;
import com.mchub.models.VoiceLesson;
import com.mchub.repositories.CompetitionRecordRepository;
import com.mchub.repositories.CompetitionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.UserStatsRepository;
import com.mchub.repositories.VoiceLessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CommunityServiceImpl. Covers leaderboard sort-type dispatch
 * (streak/precision/sessions/default/weekly), rank calculation (offset by
 * page + 1-indexed), user-rank lookup, and active arena leaderboard sorting
 * by accuracy+rhythm.
 */
@ExtendWith(MockitoExtension.class)
class CommunityServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserStatsRepository userStatsRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionRecordRepository competitionRecordRepository;
    @Mock private VoiceLessonRepository voiceLessonRepository;

    private CommunityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommunityServiceImpl(userRepository, userStatsRepository, competitionRepository,
                competitionRecordRepository, voiceLessonRepository);
    }

    private UserStats stats(String userId, double hours, int sessions, double xp, double weeklyXp, int streak) {
        return UserStats.builder().userId(userId).totalPracticeHours(hours).totalSessions(sessions)
                .cumulativeXP(xp).weeklyXP(weeklyXp).currentStreak(streak).currentTier("BRONZE").build();
    }

    @Nested
    @DisplayName("getCommunityStats")
    class GetCommunityStats {

        @Test
        @DisplayName("sums totalPracticeHours across all users")
        void sumsTotalPracticeHours() {
            when(userRepository.count()).thenReturn(100L);
            when(userStatsRepository.findAll()).thenReturn(List.of(
                    stats("u1", 5.0, 10, 0, 0, 0), stats("u2", 3.0, 8, 0, 0, 0)));
            when(voiceLessonRepository.findAll()).thenReturn(List.of());
            when(competitionRepository.findByActive(true)).thenReturn(List.of());

            CommunityStatsDTO result = service.getCommunityStats();

            assertThat(result.getTotalPracticeHours()).isEqualTo(8.0);
            assertThat(result.getTotalUsers()).isEqualTo(100L);
        }

        @Test
        @DisplayName("uses default popular script title when no lessons exist")
        void usesDefaultTitleWhenNoLessons() {
            when(userRepository.count()).thenReturn(0L);
            when(userStatsRepository.findAll()).thenReturn(List.of());
            when(voiceLessonRepository.findAll()).thenReturn(List.of());
            when(competitionRepository.findByActive(true)).thenReturn(List.of());

            CommunityStatsDTO result = service.getCommunityStats();

            assertThat(result.getMostPopularScriptTitle()).isEqualTo("Bản tin thời sự tổng hợp MCHub");
        }

        @Test
        @DisplayName("uses first lesson's title when lessons exist")
        void usesFirstLessonTitle() {
            when(userRepository.count()).thenReturn(0L);
            when(userStatsRepository.findAll()).thenReturn(List.of());
            when(voiceLessonRepository.findAll()).thenReturn(List.of(
                    VoiceLesson.builder().id("l1").title("Custom Lesson Title").build()));
            when(competitionRepository.findByActive(true)).thenReturn(List.of());

            CommunityStatsDTO result = service.getCommunityStats();

            assertThat(result.getMostPopularScriptTitle()).isEqualTo("Custom Lesson Title");
        }
    }

    @Nested
    @DisplayName("getLeaderboard — sort-type dispatch")
    class GetLeaderboardDispatch {

        @Test
        @DisplayName("weekly period always sorts by weeklyXP, ignoring type")
        void weeklyPeriodSortsByWeeklyXp() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<UserStats> page = new PageImpl<>(List.of(stats("u1", 0, 0, 0, 50, 0)), pageable, 1);
            when(userStatsRepository.findAllByOrderByWeeklyXPDesc(pageable)).thenReturn(page);
            when(userRepository.findById("u1")).thenReturn(Optional.empty());

            Page<LeaderboardEntryDTO> result = service.getLeaderboard("streak", "weekly", pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("type=streak sorts by currentStreak")
        void typeStreakSortsByCurrentStreak() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<UserStats> page = new PageImpl<>(List.of(), pageable, 0);
            when(userStatsRepository.findAllByOrderByCurrentStreakDesc(pageable)).thenReturn(page);

            service.getLeaderboard("streak", "alltime", pageable);

            org.mockito.Mockito.verify(userStatsRepository).findAllByOrderByCurrentStreakDesc(pageable);
        }

        @Test
        @DisplayName("type=precision sorts by cumulativeXP")
        void typePrecisionSortsByCumulativeXp() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userStatsRepository.findAllByOrderByCumulativeXPDesc(pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            service.getLeaderboard("precision", "alltime", pageable);

            org.mockito.Mockito.verify(userStatsRepository).findAllByOrderByCumulativeXPDesc(pageable);
        }

        @Test
        @DisplayName("type=sessions sorts by totalSessions")
        void typeSessionsSortsByTotalSessions() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userStatsRepository.findAllByOrderByTotalSessionsDesc(pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            service.getLeaderboard("sessions", "alltime", pageable);

            org.mockito.Mockito.verify(userStatsRepository).findAllByOrderByTotalSessionsDesc(pageable);
        }

        @Test
        @DisplayName("unrecognized type falls back to totalPracticeHours")
        void unrecognizedTypeFallsBackToPracticeHours() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userStatsRepository.findAllByOrderByTotalPracticeHoursDesc(pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            service.getLeaderboard("gibberish", "alltime", pageable);

            org.mockito.Mockito.verify(userStatsRepository).findAllByOrderByTotalPracticeHoursDesc(pageable);
        }

        @Test
        @DisplayName("rank is offset by page number (page 2, size 10 -> ranks start at 21)")
        void rankOffsetByPage() {
            Pageable pageable = PageRequest.of(2, 10); // offset = 20
            Page<UserStats> page = new PageImpl<>(List.of(stats("u1", 1, 1, 1, 1, 1)), pageable, 30);
            when(userStatsRepository.findAllByOrderByTotalPracticeHoursDesc(pageable)).thenReturn(page);
            when(userRepository.findById("u1")).thenReturn(Optional.empty());

            Page<LeaderboardEntryDTO> result = service.getLeaderboard("hours", "alltime", pageable);

            assertThat(result.getContent().get(0).getRank()).isEqualTo(21);
        }

        @Test
        @DisplayName("uses fallback name 'Học viên' when user record is missing")
        void fallsBackToDefaultName() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<UserStats> page = new PageImpl<>(List.of(stats("ghost", 1, 1, 1, 1, 1)), pageable, 1);
            when(userStatsRepository.findAllByOrderByTotalPracticeHoursDesc(pageable)).thenReturn(page);
            when(userRepository.findById("ghost")).thenReturn(Optional.empty());

            Page<LeaderboardEntryDTO> result = service.getLeaderboard("hours", "alltime", pageable);

            assertThat(result.getContent().get(0).getUserName()).isEqualTo("Học viên");
        }
    }

    @Nested
    @DisplayName("getUserRank")
    class GetUserRank {

        @Test
        @DisplayName("returns 1-indexed rank matching userId's position in the sorted list")
        void returnsCorrectRank() {
            when(userStatsRepository.findAllByOrderByTotalPracticeHoursDesc(Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(List.of(
                            stats("u1", 10, 0, 0, 0, 0),
                            stats("u2", 5, 0, 0, 0, 0),
                            stats("u3", 1, 0, 0, 0, 0))));
            when(userRepository.findById("u2")).thenReturn(Optional.empty());

            LeaderboardEntryDTO result = service.getUserRank("u2", "hours", "alltime");

            assertThat(result.getRank()).isEqualTo(2);
        }

        @Test
        @DisplayName("returns null when userId is not found in the leaderboard")
        void returnsNullWhenNotFound() {
            when(userStatsRepository.findAllByOrderByTotalPracticeHoursDesc(Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(List.of(stats("u1", 10, 0, 0, 0, 0))));

            LeaderboardEntryDTO result = service.getUserRank("unknown-user", "hours", "alltime");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getActiveArenas")
    class GetActiveArenas {

        @Test
        @DisplayName("sorts arena leaderboard by bestAccuracy+bestRhythm descending")
        void sortsLeaderboardByAccuracyPlusRhythm() {
            Competition comp = Competition.builder().id("comp-1").type(CompetitionType.WEEKLY)
                    .challengeScriptId("lesson-1").active(true).build();
            when(competitionRepository.findByActive(true)).thenReturn(List.of(comp));
            when(voiceLessonRepository.findById("lesson-1"))
                    .thenReturn(Optional.of(VoiceLesson.builder().id("lesson-1").title("Title").content("Content").build()));

            CompetitionRecord low = CompetitionRecord.builder().userId("u-low").userName("Low")
                    .bestAccuracy(50.0).bestRhythm(50.0).build();
            CompetitionRecord high = CompetitionRecord.builder().userId("u-high").userName("High")
                    .bestAccuracy(90.0).bestRhythm(90.0).build();
            when(competitionRecordRepository.findByCompetitionId("comp-1")).thenReturn(List.of(low, high));

            List<ActiveArenaResponseDTO> result = service.getActiveArenas(null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLeaderboard().get(0).getUserId()).isEqualTo("u-high");
            assertThat(result.get(0).getLeaderboard().get(1).getUserId()).isEqualTo("u-low");
        }

        @Test
        @DisplayName("finds and attaches the requesting user's own record")
        void attachesRequestingUsersRecord() {
            Competition comp = Competition.builder().id("comp-1").type(CompetitionType.WEEKLY)
                    .challengeScriptId("lesson-1").active(true).build();
            when(competitionRepository.findByActive(true)).thenReturn(List.of(comp));
            when(voiceLessonRepository.findById("lesson-1")).thenReturn(Optional.empty());

            CompetitionRecord myRecord = CompetitionRecord.builder().userId("me").userName("Me")
                    .bestAccuracy(80.0).bestRhythm(80.0).build();
            when(competitionRecordRepository.findByCompetitionId("comp-1")).thenReturn(List.of(myRecord));

            List<ActiveArenaResponseDTO> result = service.getActiveArenas("me");

            assertThat(result.get(0).getUserRecord()).isNotNull();
            assertThat(result.get(0).getUserRecord().getUserId()).isEqualTo("me");
        }

        @Test
        @DisplayName("userRecord is null when userId param is null (public/unauthenticated view)")
        void userRecordNullWhenUserIdNull() {
            Competition comp = Competition.builder().id("comp-1").type(CompetitionType.WEEKLY).active(true).build();
            when(competitionRepository.findByActive(true)).thenReturn(List.of(comp));
            when(competitionRecordRepository.findByCompetitionId("comp-1")).thenReturn(List.of());

            List<ActiveArenaResponseDTO> result = service.getActiveArenas(null);

            assertThat(result.get(0).getUserRecord()).isNull();
        }

        @Test
        @DisplayName("uses default challenge title when competition has no challengeScriptId")
        void usesDefaultTitleWhenNoChallengeScript() {
            Competition comp = Competition.builder().id("comp-1").type(CompetitionType.DAILY)
                    .challengeScriptId(null).active(true).build();
            when(competitionRepository.findByActive(true)).thenReturn(List.of(comp));
            when(competitionRecordRepository.findByCompetitionId("comp-1")).thenReturn(List.of());

            List<ActiveArenaResponseDTO> result = service.getActiveArenas(null);

            assertThat(result.get(0).getChallengeScriptTitle()).isEqualTo("Thử thách phát sóng");
        }
    }
}
