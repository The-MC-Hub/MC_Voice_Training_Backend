package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.NotificationResponseDTO;
import com.mchub.dto.SendNotificationRequest;
import com.mchub.enums.AuditAction;
import com.mchub.enums.NotificationType;
import com.mchub.enums.UserRole;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.mapper.NotificationMapper;
import com.mchub.models.User;
import com.mchub.repositories.NotificationRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.AuditLogService;
import com.mchub.services.NotificationService;
import com.mchub.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminNotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getAllNotifications() {
        List<NotificationResponseDTO> dtos = notificationRepository.findAll()
                .stream().map(notificationMapper::toResponseDTO).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", notificationRepository.count());
        stats.put("unreadCount", notificationRepository.countByRead(false));

        Map<String, Long> byType = new HashMap<>();
        for (NotificationType type : NotificationType.values()) {
            byType.put(type.name(), notificationRepository.countByType(type));
        }
        stats.put("byType", byType);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendNotification(
            @RequestBody @Valid SendNotificationRequest req,
            HttpServletRequest request) {

        List<String> userIds = resolveTargetUserIds(req);

        for (String userId : userIds) {
            notificationService.notify(userId, req.getType(), req.getTitle(), req.getBody(),
                    req.getActionUrl(), false);
        }

        String adminId = SecurityUtils.getCurrentUserId();
        auditLogService.log(adminId, AuditAction.ADMIN_SEND_NOTIFICATION, "Notification", null,
                "{\"targetType\":\"" + req.getTargetType() + "\",\"recipientCount\":" + userIds.size() + "}",
                request);

        return ResponseEntity.ok(ApiResponse.success("Notification sent",
                Map.of("recipientCount", userIds.size())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable String id, HttpServletRequest request) {
        if (!notificationRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Notification not found: " + id);
        }
        notificationRepository.deleteById(id);

        String adminId = SecurityUtils.getCurrentUserId();
        auditLogService.log(adminId, AuditAction.ADMIN_DELETE_NOTIFICATION, "Notification", id, null, request);

        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }

    private List<String> resolveTargetUserIds(SendNotificationRequest req) {
        String targetType = req.getTargetType().toUpperCase();
        return switch (targetType) {
            case "USER" -> {
                if (req.getUserId() == null || req.getUserId().isBlank()) {
                    throw new AppException(ErrorCode.VALIDATION_FAILED, "userId is required when targetType=USER");
                }
                yield List.of(req.getUserId());
            }
            case "ROLE" -> {
                if (req.getRole() == null || req.getRole().isBlank()) {
                    throw new AppException(ErrorCode.VALIDATION_FAILED, "role is required when targetType=ROLE");
                }
                UserRole role;
                try {
                    role = UserRole.valueOf(req.getRole().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new AppException(ErrorCode.VALIDATION_FAILED, "Invalid role: " + req.getRole());
                }
                yield userRepository.findByRole(role).stream().map(User::getId).toList();
            }
            case "ALL" -> userRepository.findByRoleNot(UserRole.ADMIN).stream().map(User::getId).toList();
            default -> throw new AppException(ErrorCode.VALIDATION_FAILED, "Invalid targetType: " + req.getTargetType());
        };
    }
}
