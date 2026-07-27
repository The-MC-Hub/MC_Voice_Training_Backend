package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.AuditLogResponseDTO;
import com.mchub.enums.AuditAction;
import com.mchub.mapper.AuditLogMapper;
import com.mchub.services.AuditLogService;
import com.mchub.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AuditLogController {

  private final AuditLogService auditLogService;
  private final AuditLogMapper auditLogMapper;

  @GetMapping
  public ResponseEntity<ApiResponse<List<AuditLogResponseDTO>>> getAllLogs() {
    List<AuditLogResponseDTO> dtos =
        auditLogService.getAllLogs().stream().map(auditLogMapper::toResponseDTO).toList();
    return ResponseEntity.ok(ApiResponse.success(dtos));
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<ApiResponse<List<AuditLogResponseDTO>>> getUserLogs(
      @PathVariable String userId) {
    List<AuditLogResponseDTO> dtos =
        auditLogService.getUserLogs(Objects.requireNonNull(userId)).stream()
            .map(auditLogMapper::toResponseDTO)
            .toList();
    return ResponseEntity.ok(ApiResponse.success(dtos));
  }

  /**
   * Purge logs older than {@code days} days. Minimum enforced server-side: 3 days (cannot erase
   * recent evidence).
   */
  @DeleteMapping("/purge")
  public ResponseEntity<ApiResponse<Map<String, Object>>> purgeLogs(
      @RequestParam(defaultValue = "30") int days, HttpServletRequest request) {
    long deleted = auditLogService.purgeLogs(days);
    int safeDays = Math.max(days, 3);
    auditLogService.log(
        SecurityUtils.getCurrentUserId(),
        AuditAction.ADMIN_PURGE_LOGS,
        "AuditLog",
        null,
        "{\"requestedDays\":"
            + days
            + ",\"enforcedDays\":"
            + safeDays
            + ",\"deleted\":"
            + deleted
            + "}",
        request);
    return ResponseEntity.ok(
        ApiResponse.success(
            "Purge completed", Map.of("deleted", deleted, "olderThanDays", safeDays)));
  }

  @GetMapping("/export-csv")
  public ResponseEntity<String> exportCsv() {
    List<com.mchub.models.AuditLog> logs = auditLogService.getAllLogs();
    StringBuilder csv = new StringBuilder();
    csv.append("ID,CreatedAt,UserId,Action,Resource,ResourceId,IP,Details\n");
    for (com.mchub.models.AuditLog log : logs) {
      csv.append(
          String.format(
              "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
              log.getId(),
              log.getCreatedAt(),
              log.getUserId(),
              log.getAction(),
              log.getResource() != null ? log.getResource() : "",
              log.getResourceId() != null ? log.getResourceId() : "",
              log.getIpAddress() != null ? log.getIpAddress() : "",
              log.getDetails() != null ? log.getDetails().replace("\"", "\"\"") : ""));
    }
    return ResponseEntity.ok()
        .header(
            org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=audit-logs.csv")
        .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
        .body(csv.toString());
  }
}
