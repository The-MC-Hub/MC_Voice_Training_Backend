package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.enums.ReportStatus;
import com.mchub.models.Report;
import com.mchub.services.ReportService;
import com.mchub.mapper.ReportMapper;
import com.mchub.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportMapper reportMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponseDTO>> createReport(
            @RequestBody @Valid CreateReportRequest req) {
        String userId = SecurityUtils.getCurrentUserId();
        Report report = reportService.createReport(Objects.requireNonNull(req), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Report submitted successfully", reportMapper.toResponseDTO(report)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReportResponseDTO>>> getMyReports() {
        String userId = SecurityUtils.getCurrentUserId();
        List<ReportResponseDTO> dtos = reportService.getMyReports(userId)
            .stream().map(reportMapper::toResponseDTO).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<ReportResponseDTO>>> getAllReports(
            @RequestParam(required = false) String status) {
        List<Report> reports = "pending".equalsIgnoreCase(status)
            ? reportService.getPendingReports()
            : reportService.getAllReports();
        List<ReportResponseDTO> dtos = reports.stream().map(reportMapper::toResponseDTO).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> resolveReport(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
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
        Report resolved = reportService.resolveReport(Objects.requireNonNull(id), adminId, reportStatus, adminNote);
        return ResponseEntity.ok(ApiResponse.success("Processed successfully", reportMapper.toResponseDTO(resolved)));
    }
}
