package com.mchub.services.impl;

import com.mchub.dto.CreateReportRequest;
import com.mchub.enums.ReportReason;
import com.mchub.enums.ReportStatus;
import com.mchub.models.Report;
import com.mchub.repositories.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReportServiceImpl. Also documents a known style finding from
 * the audit (Remaining_Modules_Audit_Report.md 3.2): resolveReport() throws a
 * raw RuntimeException instead of AppException(RESOURCE_NOT_FOUND), which means
 * GlobalExceptionHandler maps it to HTTP 500 instead of 404. Not fixed here —
 * QA reports, does not fix production code.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private ReportRepository reportRepository;

    private ReportServiceImpl service;

    private static final String REPORT_ID = "report-001";

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(reportRepository);
    }

    @Nested
    @DisplayName("createReport")
    class CreateReport {

        @Test
        @DisplayName("builds a PENDING report with the given reporterId")
        void buildsPendingReport() {
            CreateReportRequest req = new CreateReportRequest();
            req.setReportedId("user-target");
            req.setReason(ReportReason.SPAM);
            req.setDescription("spamming chat");
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

            Report result = service.createReport(req, "reporter-1");

            assertThat(result.getReporterId()).isEqualTo("reporter-1");
            assertThat(result.getReportedId()).isEqualTo("user-target");
            assertThat(result.getStatus()).isEqualTo(ReportStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("resolveReport")
    class ResolveReport {

        @Test
        @DisplayName("updates status, adminNote, resolvedBy, and sets resolvedAt")
        void updatesReportOnResolve() {
            Report report = Report.builder().id(REPORT_ID).status(ReportStatus.PENDING).build();
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

            Report result = service.resolveReport(REPORT_ID, "admin-1", ReportStatus.RESOLVED, "handled");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.RESOLVED);
            assertThat(result.getAdminNote()).isEqualTo("handled");
            assertThat(result.getResolvedBy()).isEqualTo("admin-1");
            assertThat(result.getResolvedAt()).isNotNull();
        }

        @Test
        @DisplayName("FINDING: throws raw RuntimeException (not AppException) for unknown id — see class javadoc")
        void throwsRawRuntimeExceptionForUnknownId() {
            when(reportRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolveReport("missing", "admin-1", ReportStatus.RESOLVED, "note"))
                    .isInstanceOf(RuntimeException.class)
                    .isNotInstanceOf(com.mchub.exception.AppException.class)
                    .hasMessage("Report does not exist");
        }
    }

    @Nested
    @DisplayName("queries")
    class Queries {

        @Test
        @DisplayName("getMyReports delegates to findByReporterId")
        void delegatesMyReports() {
            service.getMyReports("reporter-1");
            org.mockito.Mockito.verify(reportRepository).findByReporterId("reporter-1");
        }

        @Test
        @DisplayName("getPendingReports filters by PENDING status")
        void delegatesPendingReports() {
            service.getPendingReports();
            org.mockito.Mockito.verify(reportRepository).findByStatus(ReportStatus.PENDING);
        }

        @Test
        @DisplayName("getAllReports delegates to findAll")
        void delegatesAllReports() {
            service.getAllReports();
            org.mockito.Mockito.verify(reportRepository).findAll();
        }
    }
}
