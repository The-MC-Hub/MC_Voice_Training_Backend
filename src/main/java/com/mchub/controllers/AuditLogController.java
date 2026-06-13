package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.AuditLogResponseDTO;
import com.mchub.enums.AuditAction;
import com.mchub.services.AuditLogService;
import com.mchub.mapper.AuditLogMapper;
import com.mchub.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AuditLogMapper auditLogMapper;

        @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponseDTO>>> getAllLogs() {
        List<AuditLogResponseDTO> dtos = auditLogService.getAllLogs()
                .stream().map(auditLogMapper::toResponseDTO).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

        @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDTO>>> getUserLogs(@PathVariable String userId) {
        List<AuditLogResponseDTO> dtos = auditLogService.getUserLogs(Objects.requireNonNull(userId))
                .stream().map(auditLogMapper::toResponseDTO).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Purge logs older than {@code days} days.
     * Minimum enforced server-side: 3 days (cannot erase recent evidence).
     */
    @DeleteMapping("/purge")
    public ResponseEntity<ApiResponse<Map<String, Object>>> purgeLogs(
            @RequestParam(defaultValue = "30") int days,
            HttpServletRequest request) {
        long deleted = auditLogService.purgeLogs(days);
        int safeDays = Math.max(days, 3);
        auditLogService.log(SecurityUtils.getCurrentUserId(), AuditAction.ADMIN_PURGE_LOGS,
                "AuditLog", null,
                "{\"requestedDays\":" + days + ",\"enforcedDays\":" + safeDays + ",\"deleted\":" + deleted + "}",
                request);
        return ResponseEntity.ok(ApiResponse.success("Purge completed",
                Map.of("deleted", deleted, "olderThanDays", safeDays)));
    }
}
