package com.mchub.services.impl;

import com.mchub.enums.AuditAction;
import com.mchub.models.AuditLog;
import com.mchub.repositories.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuditLogServiceImpl. Regression guard for purgeLogs()'s
 * "never delete logs younger than 3 days" safety floor — verified safe during
 * UC-09 manual system testing (TC-ADM-*) before this JUnit coverage existed.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private HttpServletRequest request;

    private AuditLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuditLogServiceImpl(auditLogRepository);
    }

    @Nested
    @DisplayName("log")
    class Log {

        @Test
        @DisplayName("builds a SUCCESS AuditLog with client IP from X-Forwarded-For when present")
        void usesForwardedForHeader() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

            service.log("user-1", AuditAction.AUTH_LOGIN, "User", "user-1", "logged in", request);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            org.mockito.Mockito.verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.5");
            assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("falls back to remoteAddr when X-Forwarded-For is absent")
        void fallsBackToRemoteAddr() {
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            service.log("user-1", AuditAction.AUTH_LOGIN, "User", "user-1", "logged in", request);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            org.mockito.Mockito.verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("uses \"unknown\" IP when request is null")
        void unknownIpWhenRequestNull() {
            service.log("user-1", AuditAction.AUTH_LOGIN, "User", "user-1", "logged in", null);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            org.mockito.Mockito.verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("unknown");
            assertThat(captor.getValue().getUserAgent()).isNull();
        }
    }

    @Nested
    @DisplayName("logError")
    class LogError {

        @Test
        @DisplayName("builds a FAILED AuditLog with the error message")
        void buildsFailedLog() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            service.logError("user-1", AuditAction.AUTH_LOGIN, "User", "invalid password", request);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            org.mockito.Mockito.verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
            assertThat(captor.getValue().getErrorMessage()).isEqualTo("invalid password");
        }
    }

    @Nested
    @DisplayName("purgeLogs — 3-day safety floor")
    class PurgeLogs {

        @Test
        @DisplayName("enforces a minimum of 3 days even when caller requests fewer")
        void enforcesThreeDayFloor() {
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            when(auditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(5L);

            service.purgeLogs(1); // caller asks for 1 day — must be floored to 3

            org.mockito.Mockito.verify(auditLogRepository).deleteByCreatedAtBefore(cutoffCaptor.capture());
            LocalDateTime cutoff = cutoffCaptor.getValue();
            LocalDateTime expectedFloor = LocalDateTime.now().minusDays(3);
            // cutoff should be close to now-3days, NOT now-1day
            assertThat(cutoff).isBeforeOrEqualTo(expectedFloor.plusSeconds(5));
            assertThat(cutoff).isAfterOrEqualTo(expectedFloor.minusSeconds(5));
        }

        @Test
        @DisplayName("uses caller's value when it exceeds the 3-day floor")
        void usesCallerValueWhenAboveFloor() {
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            when(auditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(10L);

            service.purgeLogs(30);

            org.mockito.Mockito.verify(auditLogRepository).deleteByCreatedAtBefore(cutoffCaptor.capture());
            LocalDateTime expected30 = LocalDateTime.now().minusDays(30);
            assertThat(cutoffCaptor.getValue()).isCloseTo(expected30, org.assertj.core.api.Assertions.within(5, java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("returns the count of deleted rows from the repository")
        void returnsDeletedCount() {
            when(auditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(42L);

            long result = service.purgeLogs(90);

            assertThat(result).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("getUserLogs / getAllLogs")
    class Queries {

        @Test
        @DisplayName("getUserLogs delegates to findByUserIdOrderByCreatedAtDesc")
        void delegatesUserLogs() {
            service.getUserLogs("user-1");
            org.mockito.Mockito.verify(auditLogRepository).findByUserIdOrderByCreatedAtDesc("user-1");
        }

        @Test
        @DisplayName("getAllLogs delegates to findAllByOrderByCreatedAtDesc")
        void delegatesAllLogs() {
            service.getAllLogs();
            org.mockito.Mockito.verify(auditLogRepository).findAllByOrderByCreatedAtDesc();
        }
    }
}
