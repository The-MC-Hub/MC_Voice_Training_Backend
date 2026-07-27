package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.enums.NotificationType;
import com.mchub.enums.ReportStatus;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.mapper.ReportMapper;
import com.mchub.models.Report;
import com.mchub.services.NotificationService;
import com.mchub.services.ReportService;
import com.mchub.util.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;
  private final ReportMapper reportMapper;
  private final NotificationService notificationService;

  @PostMapping
  public ResponseEntity<ApiResponse<ReportResponseDTO>> createReport(
      @RequestBody @Valid CreateReportRequest req) {
    String userId = SecurityUtils.getCurrentUserId();
    Report report = reportService.createReport(Objects.requireNonNull(req), userId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                "Report submitted successfully", reportMapper.toResponseDTO(report)));
  }

  @GetMapping("/my")
  public ResponseEntity<ApiResponse<List<ReportResponseDTO>>> getMyReports() {
    String userId = SecurityUtils.getCurrentUserId();
    List<ReportResponseDTO> dtos =
        reportService.getMyReports(userId).stream().map(reportMapper::toResponseDTO).toList();
    return ResponseEntity.ok(ApiResponse.success(dtos));
  }

  @GetMapping("/admin")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<ApiResponse<List<ReportResponseDTO>>> getAllReports(
      @RequestParam(required = false) String status) {
    List<Report> reports =
        "pending".equalsIgnoreCase(status)
            ? reportService.getPendingReports()
            : reportService.getAllReports();
    List<ReportResponseDTO> dtos = reports.stream().map(reportMapper::toResponseDTO).toList();
    return ResponseEntity.ok(ApiResponse.success(dtos));
  }

  @PutMapping("/{id}/resolve")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<ApiResponse<ReportResponseDTO>> resolveReport(
      @PathVariable String id, @RequestBody Map<String, String> body) {
    String adminId = SecurityUtils.getCurrentUserId();
    String statusStr = body.get("status");
    if (statusStr == null || statusStr.isBlank()) {
      throw new AppException(ErrorCode.VALIDATION_FAILED, "Field 'status' cannot be empty");
    }
    ReportStatus reportStatus;
    try {
      reportStatus = ReportStatus.valueOf(statusStr.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new AppException(ErrorCode.VALIDATION_FAILED, "Invalid status: " + statusStr);
    }
    String adminNote = body.getOrDefault("adminNote", "");
    Report resolved =
        reportService.resolveReport(Objects.requireNonNull(id), adminId, reportStatus, adminNote);

    notificationService.notify(
        resolved.getReporterId(),
        NotificationType.REPORT_RESOLVED,
        "Báo cáo của bạn đã được xử lý",
        "Trạng thái: " + reportStatus.name() + (adminNote.isBlank() ? "" : " — " + adminNote),
        "/m/reports",
        false);

    return ResponseEntity.ok(
        ApiResponse.success("Processed successfully", reportMapper.toResponseDTO(resolved)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable String id) {
    reportService.deleteReport(id);
    return ResponseEntity.ok(ApiResponse.success("Report deleted successfully", null));
  }

  @PutMapping("/bulk-resolve")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> bulkResolve(
      @RequestBody Map<String, Object> body) {
    String adminId = SecurityUtils.getCurrentUserId();
    @SuppressWarnings("unchecked")
    List<String> ids = (List<String>) body.get("ids");
    String statusStr = (String) body.get("status");
    String adminNote = (String) body.getOrDefault("adminNote", "");

    if (ids == null || ids.isEmpty() || statusStr == null) {
      throw new AppException(ErrorCode.VALIDATION_FAILED, "ids and status are required");
    }
    ReportStatus status;
    try {
      status = ReportStatus.valueOf(statusStr.toUpperCase());
    } catch (Exception e) {
      throw new AppException(ErrorCode.VALIDATION_FAILED, "Invalid status: " + statusStr);
    }

    int count = reportService.bulkResolveReports(ids, adminId, status, adminNote);
    return ResponseEntity.ok(
        ApiResponse.success("Bulk resolve complete", Map.of("resolvedCount", count)));
  }

  @GetMapping("/admin/stats")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
    return ResponseEntity.ok(ApiResponse.success(reportService.getReportStats()));
  }
}
