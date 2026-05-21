package com.mchub.services;

import com.mchub.dto.CreateReportRequest;
import com.mchub.enums.ReportStatus;
import com.mchub.models.Report;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ReportService {

        @PreAuthorize("isAuthenticated()")
    Report createReport(CreateReportRequest req, String reporterId);

        @PreAuthorize("hasAuthority('ADMIN')")
    Report resolveReport(String reportId, String adminId, ReportStatus status, String adminNote);

    @PreAuthorize("hasAuthority('ADMIN') or #reporterId == authentication.name")
    List<Report> getMyReports(String reporterId);

    @PreAuthorize("hasAuthority('ADMIN')")
    List<Report> getPendingReports();

    @PreAuthorize("hasAuthority('ADMIN')")
    List<Report> getAllReports();
}
