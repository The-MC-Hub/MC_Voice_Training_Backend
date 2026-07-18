package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.LoginStreakDTO;
import com.mchub.models.UserStats;
import com.mchub.services.GamificationService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final GamificationService gamificationService;

    // ── Streak frame thresholds (days) ────────────────────────────────────────
    private static final int SPARK_DAYS    = 3;
    private static final int FLAME_DAYS    = 7;
    private static final int STORM_DAYS    = 14;
    private static final int LEGEND_DAYS   = 30;
    private static final int ELITE_DAYS    = 60;
    private static final int IMMORTAL_DAYS = 100;

    @GetMapping("/me/practice-stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserStats>> getPracticeStats() {
        String userId = SecurityUtils.getCurrentUserId();
        UserStats stats = gamificationService.getOrCreateUserStats(userId);
        return ResponseEntity.ok(ApiResponse.success("OK", stats));
    }

    @GetMapping("/me/streak")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LoginStreakDTO>> getLoginStreak() {
        String userId = SecurityUtils.getCurrentUserId();
        UserStats stats = gamificationService.getOrCreateUserStats(userId);

        int streak = stats.getLoginStreak();
        String frame = resolveFrame(streak);
        String nextFrame = resolveNextFrame(streak);
        int daysToNext = daysToNextFrame(streak);

        LoginStreakDTO dto = LoginStreakDTO.builder()
                .loginStreak(streak)
                .longestLoginStreak(stats.getLongestLoginStreak())
                .freezesAvailable(stats.getFreezesAvailable())
                .lastLoginDate(stats.getLastLoginDate())
                .streakFrame(frame)
                .nextFrame(nextFrame)
                .daysToNextFrame(daysToNext)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Streak retrieved", dto));
    }

    /**
     * Freeze is consumed automatically in processLoginStreak() when a user logs in
     * after skipping exactly one day (gap==2) — there is no separate action to take
     * here. Both routes below just report the current freeze status so the client
     * can display it; POST is kept for backward compatibility with existing callers,
     * GET is the more semantically correct alias for a read-only status check.
     */
    @PostMapping("/me/streak/freeze")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LoginStreakDTO>> useFreeze() {
        return getFreezeStatus();
    }

    @GetMapping("/me/streak/freeze-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LoginStreakDTO>> getFreezeStatus() {
        String userId = SecurityUtils.getCurrentUserId();
        UserStats stats = gamificationService.getOrCreateUserStats(userId);

        if (stats.getFreezesAvailable() <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Không còn lượt freeze. Freeze được nạp lại vào đầu tháng."));
        }
        return getLoginStreak();
    }

    // ── Frame resolution helpers ──────────────────────────────────────────────

    private static String resolveFrame(int streak) {
        if (streak >= IMMORTAL_DAYS) return "IMMORTAL";
        if (streak >= ELITE_DAYS)    return "ELITE";
        if (streak >= LEGEND_DAYS)   return "LEGEND";
        if (streak >= STORM_DAYS)    return "STORM";
        if (streak >= FLAME_DAYS)    return "FLAME";
        if (streak >= SPARK_DAYS)    return "SPARK";
        return "NONE";
    }

    private static String resolveNextFrame(int streak) {
        if (streak >= IMMORTAL_DAYS) return null;
        if (streak >= ELITE_DAYS)    return "IMMORTAL";
        if (streak >= LEGEND_DAYS)   return "ELITE";
        if (streak >= STORM_DAYS)    return "LEGEND";
        if (streak >= FLAME_DAYS)    return "STORM";
        if (streak >= SPARK_DAYS)    return "FLAME";
        return "SPARK";
    }

    private static int daysToNextFrame(int streak) {
        if (streak >= IMMORTAL_DAYS) return 0;
        if (streak >= ELITE_DAYS)    return IMMORTAL_DAYS - streak;
        if (streak >= LEGEND_DAYS)   return ELITE_DAYS - streak;
        if (streak >= STORM_DAYS)    return LEGEND_DAYS - streak;
        if (streak >= FLAME_DAYS)    return STORM_DAYS - streak;
        if (streak >= SPARK_DAYS)    return FLAME_DAYS - streak;
        return SPARK_DAYS - streak;
    }
}
