package com.mchub.services.impl;

import com.mchub.models.SystemLog;
import com.mchub.repositories.SystemLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for LogServiceImpl. Covers the query-dispatch logic in getLogs()
 * (4-way branch on level/source presence) and the external AI-service log
 * ingest path. SSE streaming/broadcast and the @PostConstruct Java-log
 * listener wiring are integration concerns, not covered here.
 */
@ExtendWith(MockitoExtension.class)
class LogServiceImplTest {

    @Mock private SystemLogRepository logRepository;

    private LogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LogServiceImpl(logRepository);
    }

    @Nested
    @DisplayName("getLogs — dispatch by level/source presence")
    class GetLogs {

        @Test
        @DisplayName("both level and source present: uses findTop200ByLevelAndSourceOrderByTimestampDesc, uppercased")
        void bothLevelAndSource() {
            service.getLogs("error", "java", 200);

            verify(logRepository).findTop200ByLevelAndSourceOrderByTimestampDesc("ERROR", "JAVA");
        }

        @Test
        @DisplayName("only level present: uses findTop200ByLevelOrderByTimestampDesc")
        void onlyLevel() {
            service.getLogs("warn", null, 200);

            verify(logRepository).findTop200ByLevelOrderByTimestampDesc("WARN");
            verify(logRepository, never()).findTop200BySourceOrderByTimestampDesc(any());
        }

        @Test
        @DisplayName("only source present: uses findTop200BySourceOrderByTimestampDesc")
        void onlySource() {
            service.getLogs(null, "ai", 200);

            verify(logRepository).findTop200BySourceOrderByTimestampDesc("AI");
        }

        @Test
        @DisplayName("neither present: falls back to findTop200ByOrderByTimestampDesc")
        void neitherPresent() {
            service.getLogs(null, null, 200);

            verify(logRepository).findTop200ByOrderByTimestampDesc();
        }
    }

    @Nested
    @DisplayName("ingestExternal")
    class IngestExternal {

        @Test
        @DisplayName("forces source to \"AI\" regardless of the incoming value")
        void forcesSourceToAi() {
            SystemLog entry = SystemLog.builder().source("JAVA").message("from python").build();

            service.ingestExternal(entry);

            ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
            verify(logRepository).save(captor.capture());
            assertThat(captor.getValue().getSource()).isEqualTo("AI");
        }

        @Test
        @DisplayName("sets timestamp to now when not supplied")
        void setsTimestampWhenMissing() {
            SystemLog entry = SystemLog.builder().message("no timestamp").build();

            service.ingestExternal(entry);

            assertThat(entry.getTimestamp()).isNotNull();
            assertThat(entry.getTimestamp()).isCloseTo(Instant.now(), org.assertj.core.api.Assertions.within(3, java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("preserves an explicit timestamp when supplied")
        void preservesExplicitTimestamp() {
            Instant explicit = Instant.now().minusSeconds(3600);
            SystemLog entry = SystemLog.builder().message("has timestamp").timestamp(explicit).build();

            service.ingestExternal(entry);

            assertThat(entry.getTimestamp()).isEqualTo(explicit);
        }

        @Test
        @DisplayName("does not propagate an exception if the repository save fails")
        void swallowsRepositorySaveFailure() {
            org.mockito.Mockito.doThrow(new RuntimeException("DB down")).when(logRepository).save(any(SystemLog.class));

            SystemLog entry = SystemLog.builder().message("boom").build();

            service.ingestExternal(entry); // must not throw
        }
    }

    @Nested
    @DisplayName("cleanOldLogs — scheduled TTL cleanup")
    class CleanOldLogs {

        @Test
        @DisplayName("deletes logs older than 7 days")
        void deletesLogsOlderThan7Days() {
            ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);

            service.cleanOldLogs();

            verify(logRepository).deleteByTimestampBefore(cutoffCaptor.capture());
            Instant expected = Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
            assertThat(cutoffCaptor.getValue()).isCloseTo(expected, org.assertj.core.api.Assertions.within(5, java.time.temporal.ChronoUnit.SECONDS));
        }
    }
}
