package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.models.UserHighlight;
import com.mchub.services.UserHighlightService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/highlights")
@RequiredArgsConstructor
public class UserHighlightController {

    private final UserHighlightService highlightService;

    @GetMapping("/reading-guides/{guideId}")
    public ResponseEntity<ApiResponse<List<UserHighlight>>> getHighlights(@PathVariable String guideId) {
        String userId = SecurityUtils.getCurrentUserId();
        List<UserHighlight> highlights = highlightService.getHighlights(userId, guideId);
        return ResponseEntity.ok(ApiResponse.success("Highlights retrieved successfully", highlights));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserHighlight>> createHighlight(@RequestBody UserHighlight highlight) {
        String userId = SecurityUtils.getCurrentUserId();
        UserHighlight saved = highlightService.createHighlight(userId, highlight);
        return ResponseEntity.ok(ApiResponse.success("Highlight created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserHighlight>> updateHighlight(
            @PathVariable String id,
            @RequestBody UserHighlight request) {
        String userId = SecurityUtils.getCurrentUserId();
        UserHighlight updated = highlightService.updateHighlight(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Highlight updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHighlight(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        highlightService.deleteHighlight(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Highlight deleted", null));
    }
}
