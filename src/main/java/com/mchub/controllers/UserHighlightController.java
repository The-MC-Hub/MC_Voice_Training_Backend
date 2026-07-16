package com.mchub.controllers;

import com.mchub.models.UserHighlight;
import com.mchub.repositories.UserHighlightRepository;
import com.mchub.dto.ApiResponse;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/v1/highlights")
@RequiredArgsConstructor
public class UserHighlightController {

    private final UserHighlightRepository highlightRepository;

    @GetMapping("/reading-guides/{guideId}")
    public ResponseEntity<ApiResponse<Object>> getHighlights(@PathVariable String guideId) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Highlights retrieved successfully",
                highlightRepository.findByUserIdAndReadingGuideIdOrderByCreatedAtDesc(userId, guideId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserHighlight>> createHighlight(@RequestBody UserHighlight highlight) {
        String userId = SecurityUtils.getCurrentUserId();
        highlight.setUserId(userId);
        highlight.setCreatedAt(new Date());
        highlight.setUpdatedAt(new Date());
        UserHighlight saved = highlightRepository.save(highlight);
        return ResponseEntity.ok(ApiResponse.success("Highlight created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserHighlight>> updateHighlight(
            @PathVariable String id,
            @RequestBody UserHighlight request) {
        String userId = SecurityUtils.getCurrentUserId();
        UserHighlight highlight = highlightRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Highlight not found: " + id));
        if (!userId.equals(highlight.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Access denied");
        }
        if (request.getColorHex() != null) highlight.setColorHex(request.getColorHex());
        if (request.getNoteContent() != null) highlight.setNoteContent(request.getNoteContent());
        highlight.setUpdatedAt(new Date());
        return ResponseEntity.ok(ApiResponse.success("Highlight updated", highlightRepository.save(highlight)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHighlight(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        UserHighlight highlight = highlightRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Highlight not found: " + id));
        if (!userId.equals(highlight.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Access denied");
        }
        highlightRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Highlight deleted", null));
    }
}
