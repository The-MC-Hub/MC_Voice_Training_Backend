package com.mchub.services;

import com.mchub.dto.ActiveArenaResponseDTO;
import com.mchub.dto.CommunityStatsDTO;
import com.mchub.dto.LeaderboardEntryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommunityService {
    CommunityStatsDTO getCommunityStats();
    Page<LeaderboardEntryDTO> getLeaderboard(String type, String period, Pageable pageable);
    LeaderboardEntryDTO getUserRank(String userId, String type, String period);
    List<ActiveArenaResponseDTO> getActiveArenas(String userId);
}
