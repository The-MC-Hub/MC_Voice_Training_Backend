package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.services.NotificationService;
import com.mchub.mapper.NotificationMapper;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

        @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        String userId = SecurityUtils.getCurrentUserId();
        List<NotificationResponseDTO> list = notificationService.getUserNotifications(Objects.requireNonNull(userId), page, limit)
            .stream().map(notificationMapper::toResponseDTO).toList();
        long unreadCount = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "notifications", list,
            "total", list.size(),
            "unreadCount", unreadCount
        )));
    }

        @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        String userId = SecurityUtils.getCurrentUserId();
        long count = notificationService.getUnreadCount(Objects.requireNonNull(userId));
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

        @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(Objects.requireNonNull(id), Objects.requireNonNull(userId));
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

        @PostMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        String userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(Objects.requireNonNull(userId));
        return ResponseEntity.ok(ApiResponse.success("All marked as read", null));
    }

        @DeleteMapping("/delete-all")
    public ResponseEntity<ApiResponse<Void>> deleteAll() {
        String userId = SecurityUtils.getCurrentUserId();
        notificationService.deleteAll(Objects.requireNonNull(userId));
        return ResponseEntity.ok(ApiResponse.success("All notifications deleted", null));
    }
}
