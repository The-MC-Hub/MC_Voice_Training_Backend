package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.services.QuestService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    // ================================================================
    //  GET /api/v1/quests/progress
    //  Returns which quests the current user has completed
    // ================================================================
    @GetMapping("/progress")
    @PreAuthorize("hasAuthority('MC')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgress() {
        String userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> progress = questService.getProgress(userId);
        return ResponseEntity.ok(ApiResponse.success("Quest progress retrieved", progress));
    }

    // ================================================================
    //  POST /api/v1/quests/complete/{questId}
    //  Mark a quest as completed. Idempotent — safe to call multiple times.
    // ================================================================
    @PostMapping("/complete/{questId}")
    @PreAuthorize("hasAuthority('MC')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeQuest(@PathVariable String questId) {
        String userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = questService.completeQuest(userId, questId);
        return ResponseEntity.ok(ApiResponse.success("Quest completed", result));
    }

    // ================================================================
    //  POST /api/v1/quests/claim-voucher
    //  Generate a personal 50% discount code for BASIC plan.
    //  Requires all 5 quests done, FREE plan, not yet claimed.
    // ================================================================
    @PostMapping("/claim-voucher")
    @PreAuthorize("hasAuthority('MC')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> claimVoucher() {
        String userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = questService.claimVoucher(userId);
        return ResponseEntity.ok(ApiResponse.success("Voucher nhận thành công!", result));
    }
}
