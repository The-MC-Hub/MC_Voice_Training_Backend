package com.mchub.services;

import com.mchub.dto.ActiveArenaResponseDTO;
import com.mchub.dto.CommunityStatsDTO;
import com.mchub.dto.LeaderboardEntryDTO;

import java.util.List;

public interface CommunityService {
    CommunityStatsDTO getCommunityStats();
    List<LeaderboardEntryDTO> getDiligentLeaderboard();
    List<LeaderboardEntryDTO> getPrecisionLeaderboard();
    List<LeaderboardEntryDTO> getStreakLeaderboard();
    List<ActiveArenaResponseDTO> getActiveArenas(String userId);
}
