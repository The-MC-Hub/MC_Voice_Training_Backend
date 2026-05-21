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
import org.springframework.stereotype.Service;

import java.util.*;
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

        // Get the first script title as the popular script, or default if empty
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
    public List<LeaderboardEntryDTO> getDiligentLeaderboard() {
        return userStatsRepository.findAll().stream()
                .sorted(Comparator.comparingDouble(UserStats::getTotalPracticeHours).reversed())
                .limit(10)
                .map(this::mapToLeaderboardEntry)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaderboardEntryDTO> getPrecisionLeaderboard() {
        return userStatsRepository.findAll().stream()
                .sorted(Comparator.comparingDouble(UserStats::getCumulativeXP).reversed())
                .limit(10)
                .map(this::mapToLeaderboardEntry)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaderboardEntryDTO> getStreakLeaderboard() {
        return userStatsRepository.findAll().stream()
                .sorted(Comparator.comparingInt(UserStats::getCurrentStreak).reversed())
                .limit(10)
                .map(this::mapToLeaderboardEntry)
                .collect(Collectors.toList());
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

            // Fetch arena leaderboard
            List<CompetitionRecord> records = competitionRecordRepository.findByCompetitionId(comp.getId());
            List<ArenaLeaderboardEntryDTO> leaderboard = records.stream()
                    .sorted((r1, r2) -> Double.compare(r2.getBestAccuracy() + r2.getBestRhythm(), r1.getBestAccuracy() + r1.getBestRhythm()))
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

            // Fetch current user's arena performance
            ArenaLeaderboardEntryDTO userRecord = null;
            if (userId != null) {
                userRecord = leaderboard.stream()
                        .filter(entry -> entry.getUserId().equals(userId))
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

    private LeaderboardEntryDTO mapToLeaderboardEntry(UserStats stats) {
        User user = userRepository.findById(stats.getUserId()).orElse(null);
        String name = user != null ? user.getName() : "Học viên ẩn danh";
        String avatar = user != null ? user.getAvatar() : "default-avatar.png";

        return LeaderboardEntryDTO.builder()
                .userId(stats.getUserId())
                .userName(name)
                .userAvatar(avatar)
                .totalPracticeHours(stats.getTotalPracticeHours())
                .totalSessions(stats.getTotalSessions())
                .cumulativeXP(stats.getCumulativeXP())
                .currentStreak(stats.getCurrentStreak())
                .currentTier(stats.getCurrentTier())
                .build();
    }
}
