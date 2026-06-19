package com.mchub.services.impl;

import com.mchub.dto.ActiveArenaResponseDTO;
import com.mchub.dto.ArenaLeaderboardEntryDTO;
import com.mchub.dto.CommunityStatsDTO;
import com.mchub.dto.LeaderboardEntryDTO;
import com.mchub.models.*;
import com.mchub.repositories.*;
import com.mchub.services.CommunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityServiceImpl implements CommunityService {

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionRecordRepository competitionRecordRepository;
    private final VoiceLessonRepository voiceLessonRepository;

    @Override
    public CommunityStatsDTO getCommunityStats() {
        long totalUsers = userRepository.count();
        double totalPracticeHours = userStatsRepository.findAll().stream()
                .mapToDouble(UserStats::getTotalPracticeHours)
                .sum();

        String popularScript = "Bản tin thời sự tổng hợp MCHub";
        List<VoiceLesson> lessons = voiceLessonRepository.findAll();
        if (!lessons.isEmpty()) {
            popularScript = lessons.get(0).getTitle();
        }

        int activeComps = competitionRepository.findByActive(true).size();

        return CommunityStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalPracticeHours(totalPracticeHours)
                .mostPopularScriptTitle(popularScript)
                .activeCompetitionsCount(activeComps)
                .build();
    }

    @Override
    public Page<LeaderboardEntryDTO> getLeaderboard(String type, String period, Pageable pageable) {
        boolean isWeekly = "weekly".equalsIgnoreCase(period);
        Page<UserStats> statsPage;

        if (isWeekly) {
            // Weekly: all types sort by weeklyXP (proxy for weekly activity)
            statsPage = userStatsRepository.findAllByOrderByWeeklyXPDesc(pageable);
        } else {
            statsPage = switch (type.toLowerCase()) {
                case "streak"    -> userStatsRepository.findAllByOrderByCurrentStreakDesc(pageable);
                case "precision" -> userStatsRepository.findAllByOrderByCumulativeXPDesc(pageable);
                case "sessions"  -> userStatsRepository.findAllByOrderByTotalSessionsDesc(pageable);
                default          -> userStatsRepository.findAllByOrderByTotalPracticeHoursDesc(pageable);
            };
        }

        // Base rank offset from page (page 0 item 0 = rank 1)
        int rankOffset = (int) pageable.getOffset();
        AtomicInteger counter = new AtomicInteger(rankOffset + 1);

        List<LeaderboardEntryDTO> entries = statsPage.getContent().stream()
                .map(s -> mapToEntry(s, counter.getAndIncrement()))
                .collect(Collectors.toList());

        return new PageImpl<>(entries, pageable, statsPage.getTotalElements());
    }

    @Override
    public LeaderboardEntryDTO getUserRank(String userId, String type, String period) {
        boolean isWeekly = "weekly".equalsIgnoreCase(period);

        // Fetch all sorted, find position — acceptable for leaderboard (cached in prod)
        List<UserStats> all;
        if (isWeekly) {
            all = userStatsRepository.findAllByOrderByWeeklyXPDesc(Pageable.unpaged()).getContent();
        } else {
            all = switch (type.toLowerCase()) {
                case "streak"    -> userStatsRepository.findAllByOrderByCurrentStreakDesc(Pageable.unpaged()).getContent();
                case "precision" -> userStatsRepository.findAllByOrderByCumulativeXPDesc(Pageable.unpaged()).getContent();
                case "sessions"  -> userStatsRepository.findAllByOrderByTotalSessionsDesc(Pageable.unpaged()).getContent();
                default          -> userStatsRepository.findAllByOrderByTotalPracticeHoursDesc(Pageable.unpaged()).getContent();
            };
        }

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getUserId().equals(userId)) {
                return mapToEntry(all.get(i), i + 1);
            }
        }
        return null;
    }

    @Override
    public List<ActiveArenaResponseDTO> getActiveArenas(String userId) {
        List<Competition> activeComps = competitionRepository.findByActive(true);
        List<ActiveArenaResponseDTO> arenas = new ArrayList<>();

        for (Competition comp : activeComps) {
            String scriptTitle = "Thử thách phát sóng";
            String scriptContent = "";

            if (comp.getChallengeScriptId() != null) {
                Optional<VoiceLesson> lessonOpt = voiceLessonRepository.findById(comp.getChallengeScriptId());
                if (lessonOpt.isPresent()) {
                    scriptTitle = lessonOpt.get().getTitle();
                    scriptContent = lessonOpt.get().getContent();
                }
            }

            List<CompetitionRecord> records = competitionRecordRepository.findByCompetitionId(comp.getId());
            List<ArenaLeaderboardEntryDTO> leaderboard = records.stream()
                    .sorted((r1, r2) -> Double.compare(
                            r2.getBestAccuracy() + r2.getBestRhythm(),
                            r1.getBestAccuracy() + r1.getBestRhythm()))
                    .map(r -> ArenaLeaderboardEntryDTO.builder()
                            .userId(r.getUserId())
                            .userName(r.getUserName())
                            .userAvatar(r.getUserAvatar())
                            .bestAccuracy(r.getBestAccuracy())
                            .bestRhythm(r.getBestRhythm())
                            .attemptCount(r.getAttemptCount())
                            .pointsEarned(r.getPointsEarned())
                            .build())
                    .collect(Collectors.toList());

            ArenaLeaderboardEntryDTO userRecord = null;
            if (userId != null) {
                userRecord = leaderboard.stream()
                        .filter(e -> e.getUserId().equals(userId))
                        .findFirst()
                        .orElse(null);
            }

            arenas.add(ActiveArenaResponseDTO.builder()
                    .competition(comp)
                    .challengeScriptTitle(scriptTitle)
                    .challengeScriptContent(scriptContent)
                    .leaderboard(leaderboard)
                    .userRecord(userRecord)
                    .build());
        }

        return arenas;
    }

    private LeaderboardEntryDTO mapToEntry(UserStats stats, int rank) {
        User user = userRepository.findById(stats.getUserId()).orElse(null);
        return LeaderboardEntryDTO.builder()
                .rank(rank)
                .userId(stats.getUserId())
                .userName(user != null ? user.getName() : "Học viên")
                .userAvatar(user != null ? user.getAvatar() : null)
                .totalPracticeHours(stats.getTotalPracticeHours())
                .totalSessions(stats.getTotalSessions())
                .cumulativeXP(stats.getCumulativeXP())
                .weeklyXP(stats.getWeeklyXP())
                .currentStreak(stats.getCurrentStreak())
                .currentTier(stats.getCurrentTier())
                .build();
    }
}
