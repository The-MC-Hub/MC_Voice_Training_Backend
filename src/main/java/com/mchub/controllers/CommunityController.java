package com.mchub.controllers;

import com.mchub.dto.ActiveArenaResponseDTO;
import com.mchub.dto.ApiResponse;
import com.mchub.dto.CommunityStatsDTO;
import com.mchub.dto.LeaderboardEntryDTO;
import com.mchub.services.CommunityService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CommunityStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(communityService.getCommunityStats()));
    }

    @GetMapping("/leaderboards")
    public ResponseEntity<ApiResponse<Map<String, List<LeaderboardEntryDTO>>>> getLeaderboards() {
        Map<String, List<LeaderboardEntryDTO>> leaderboards = Map.of(
                "diligent", communityService.getDiligentLeaderboard(),
                "precision", communityService.getPrecisionLeaderboard(),
                "streak", communityService.getStreakLeaderboard()
        );
        return ResponseEntity.ok(ApiResponse.success(leaderboards));
    }

    @GetMapping("/active-arenas")
    public ResponseEntity<ApiResponse<List<ActiveArenaResponseDTO>>> getActiveArenas() {
        String userId = null;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // User not authenticated, fetch standard public arena info
        }
        return ResponseEntity.ok(ApiResponse.success(communityService.getActiveArenas(userId)));
    }
}
