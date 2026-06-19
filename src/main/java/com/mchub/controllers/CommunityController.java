package com.mchub.controllers;

import com.mchub.dto.ActiveArenaResponseDTO;
import com.mchub.dto.ApiResponse;
import com.mchub.dto.CommunityStatsDTO;
import com.mchub.dto.LeaderboardEntryDTO;
import com.mchub.services.CommunityService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CommunityStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(communityService.getCommunityStats()));
    }

    /**
     * GET /api/v1/community/leaderboard
     * @param type   streak | diligent | precision | sessions  (default: streak)
     * @param period all_time | weekly                         (default: all_time)
     * @param page   0-based page index                        (default: 0)
     * @param size   page size, capped at 50                   (default: 20)
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<Page<LeaderboardEntryDTO>>> getLeaderboard(
            @RequestParam(defaultValue = "streak")   String type,
            @RequestParam(defaultValue = "all_time") String period,
            @RequestParam(defaultValue = "0")        int page,
            @RequestParam(defaultValue = "20")       int size) {

        int cappedSize = Math.min(size, 50);
        Page<LeaderboardEntryDTO> result = communityService.getLeaderboard(
                type, period, PageRequest.of(page, cappedSize));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/v1/community/leaderboard/me
     * Returns the current user's rank entry for a given type+period.
     */
    @GetMapping("/leaderboard/me")
    public ResponseEntity<ApiResponse<LeaderboardEntryDTO>> getMyRank(
            @RequestParam(defaultValue = "streak")   String type,
            @RequestParam(defaultValue = "all_time") String period) {

        String userId = SecurityUtils.getCurrentUserId();
        LeaderboardEntryDTO entry = communityService.getUserRank(userId, type, period);
        return ResponseEntity.ok(ApiResponse.success(entry));
    }

    @GetMapping("/active-arenas")
    public ResponseEntity<ApiResponse<List<ActiveArenaResponseDTO>>> getActiveArenas() {
        String userId = null;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // unauthenticated
        }
        return ResponseEntity.ok(ApiResponse.success(communityService.getActiveArenas(userId)));
    }
}
