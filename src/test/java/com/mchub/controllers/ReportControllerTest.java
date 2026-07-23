package com.mchub.controllers;

import com.mchub.enums.ReportStatus;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.mapper.ReportMapper;
import com.mchub.models.Report;
import com.mchub.services.ReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class)
@ContextConfiguration(classes = {ReportController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ReportService reportService;
    @MockBean private ReportMapper reportMapper;
    @MockBean private com.mchub.services.NotificationService notificationService;

    private static final String USER_ID = "user-report-001";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/v1/reports")
    class CreateReport {

        @Test
        @DisplayName("400 when reportedId is blank — bean validation")
        void rejectsBlankReportedId() throws Exception {
            mockMvc.perform(post("/api/v1/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportedId\":\"\",\"reason\":\"SPAM\",\"description\":\"desc\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("201 CREATED with valid payload")
        void createsReport() throws Exception {
            when(reportService.createReport(any(), org.mockito.ArgumentMatchers.eq(USER_ID)))
                    .thenReturn(Report.builder().id("r1").build());

            mockMvc.perform(post("/api/v1/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportedId\":\"target\",\"reason\":\"SPAM\",\"description\":\"desc\"}"))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reports/admin — status filter")
    class GetAllReports {

        @Test
        @DisplayName("status=pending filters to getPendingReports")
        void filtersToPendingReports() throws Exception {
            when(reportService.getPendingReports()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/reports/admin").param("status", "pending")).andExpect(status().isOk());

            verify(reportService).getPendingReports();
            verify(reportService, org.mockito.Mockito.never()).getAllReports();
        }

        @Test
        @DisplayName("no status param: falls back to getAllReports")
        void fallsBackToAllReports() throws Exception {
            when(reportService.getAllReports()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/reports/admin")).andExpect(status().isOk());

            verify(reportService).getAllReports();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/reports/{id}/resolve")
    class ResolveReport {

        @Test
        @DisplayName("400 VALIDATION_FAILED when status field is missing")
        void rejectsMissingStatus() throws Exception {
            mockMvc.perform(put("/api/v1/reports/{id}/resolve", "r1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED for an unparseable status string")
        void rejectsInvalidStatus() throws Exception {
            mockMvc.perform(put("/api/v1/reports/{id}/resolve", "r1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"GARBAGE\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("200 OK with valid status")
        void resolvesWithValidStatus() throws Exception {
            when(reportService.resolveReport("r1", USER_ID, ReportStatus.RESOLVED, "note"))
                    .thenReturn(Report.builder().id("r1").build());

            mockMvc.perform(put("/api/v1/reports/{id}/resolve", "r1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"resolved\",\"adminNote\":\"note\"}"))
                    .andExpect(status().isOk());
        }
    }
}
