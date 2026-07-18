package com.mchub.services.impl;

import com.mchub.dto.CreateReportRequest;
import com.mchub.enums.ReportStatus;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Report;
import com.mchub.repositories.ReportRepository;
import com.mchub.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;

    @Override
    public Report createReport(CreateReportRequest req, String reporterId) {
        Report report = Report.builder()
            .reporterId(reporterId)
            .reportedId(req.getReportedId())
            .reason(req.getReason())
            .description(req.getDescription())
            .evidenceUrls(req.getEvidenceUrls())
            .status(ReportStatus.PENDING)
            .build();
        return reportRepository.save(Objects.requireNonNull(report));
    }

    @Override
    public Report resolveReport(String reportId, String adminId, ReportStatus status, String adminNote) {
        Report report = reportRepository.findById(Objects.requireNonNull(reportId))
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Report not found: " + reportId));
        report.setStatus(status);
        report.setAdminNote(adminNote);
        report.setResolvedBy(adminId);
        report.setResolvedAt(LocalDateTime.now());
        return reportRepository.save(Objects.requireNonNull(report));
    }

    @Override
    public List<Report> getMyReports(String reporterId) {
        return reportRepository.findByReporterId(reporterId);
    }

    @Override
    public List<Report> getPendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING);
    }

    @Override
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }
}
