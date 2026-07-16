package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.MinigameLeaderboardEntryDTO;
import com.mchub.dto.MinigamePromptDTO;
import com.mchub.dto.MinigameResultDTO;
import com.mchub.dto.MinigameSubmitRequest;
import com.mchub.services.MinigameService;
import com.mchub.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/minigames")
@RequiredArgsConstructor
public class MinigameController {

    private final MinigameService minigameService;

    /** GET /api/v1/minigames/speed-reader/prompts — fetch a shuffled round of prompts to read aloud */
    @GetMapping("/speed-reader/prompts")
    public ResponseEntity<ApiResponse<List<MinigamePromptDTO>>> getSpeedReaderPrompts(
            @RequestParam(defaultValue = "NORMAL") String difficulty,
            @RequestParam(defaultValue = "8") int rounds) {
        return ResponseEntity.ok(ApiResponse.success("Prompts retrieved",
                minigameService.getSpeedReaderPrompts(difficulty, rounds)));
    }

    /** POST /api/v1/minigames/speed-reader/submit — submit a finished run, awards XP */
    @PostMapping("/speed-reader/submit")
    public ResponseEntity<ApiResponse<MinigameResultDTO>> submitSpeedReaderRun(
            @Valid @RequestBody MinigameSubmitRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Run recorded",
                minigameService.submitSpeedReaderRun(userId, request)));
    }

    /** GET /api/v1/minigames/leaderboard — top scores for a minigame */
    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<MinigameLeaderboardEntryDTO>>> getLeaderboard(
            @RequestParam(defaultValue = "SPEED_READER") String gameType,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Leaderboard retrieved",
                minigameService.getLeaderboard(gameType, limit)));
    }
}
