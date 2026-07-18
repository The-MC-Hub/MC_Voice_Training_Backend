package com.mchub.controllers;

import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.mapper.AuditLogMapper;
import com.mchub.services.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression guard for the 3-day purge floor (also covered at the service
 * layer in AuditLogServiceImplTest) — here we confirm the controller reports
 * the enforced (floored) value back to the caller, not the raw requested one.
 */
@WebMvcTest(controllers = AuditLogController.class)
@ContextConfiguration(classes = {AuditLogController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AuditLogService auditLogService;
    @MockBean private AuditLogMapper auditLogMapper;

    private static final String ADMIN_ID = "admin-001";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ADMIN_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/audit-logs, /user/{userId}")
    class Queries {

        @Test
        @DisplayName("getAllLogs maps entities through AuditLogMapper")
        void mapsAllLogs() throws Exception {
            when(auditLogService.getAllLogs()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/audit-logs")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("getUserLogs delegates with the pathVariable userId")
        void delegatesUserLogs() throws Exception {
            when(auditLogService.getUserLogs("target-user")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/audit-logs/user/{userId}", "target-user")).andExpect(status().isOk());

            verify(auditLogService).getUserLogs("target-user");
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/audit-logs/purge — reports the ENFORCED days, not requested")
    class PurgeLogs {

        @Test
        @DisplayName("reports enforced floor of 3 when caller requests fewer days")
        void reportsFlooredValue() throws Exception {
            when(auditLogService.purgeLogs(1)).thenReturn(5L);

            mockMvc.perform(delete("/api/v1/audit-logs/purge").param("days", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.olderThanDays").value(3))
                    .andExpect(jsonPath("$.data.deleted").value(5));
        }

        @Test
        @DisplayName("reports requested value when it exceeds the floor")
        void reportsRequestedValueWhenAboveFloor() throws Exception {
            when(auditLogService.purgeLogs(90)).thenReturn(20L);

            mockMvc.perform(delete("/api/v1/audit-logs/purge").param("days", "90"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.olderThanDays").value(90));
        }

        @Test
        @DisplayName("defaults to 30 days when not specified")
        void defaultsToThirtyDays() throws Exception {
            when(auditLogService.purgeLogs(30)).thenReturn(0L);

            mockMvc.perform(delete("/api/v1/audit-logs/purge")).andExpect(status().isOk());

            verify(auditLogService).purgeLogs(30);
        }

        @Test
        @DisplayName("logs an ADMIN_PURGE_LOGS audit entry with both requested and enforced days")
        void logsAuditEntry() throws Exception {
            when(auditLogService.purgeLogs(1)).thenReturn(5L);

            mockMvc.perform(delete("/api/v1/audit-logs/purge").param("days", "1")).andExpect(status().isOk());

            verify(auditLogService).log(eq(ADMIN_ID), any(), any(), any(),
                    org.mockito.ArgumentMatchers.contains("\"enforcedDays\":3"), any());
        }
    }
}
