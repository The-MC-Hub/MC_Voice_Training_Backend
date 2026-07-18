package com.mchub.controllers;

import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.SystemLog;
import com.mchub.services.LogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LogController.class)
@ContextConfiguration(classes = {LogController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class LogControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private LogService logService;

    @Nested
    @DisplayName("GET /api/v1/admin/logs")
    class GetLogs {

        @Test
        @DisplayName("200 OK, forwards level/source/limit params")
        void forwardsFilterParams() throws Exception {
            when(logService.getLogs("ERROR", "AI", 50)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/logs").param("level", "ERROR").param("source", "AI").param("limit", "50"))
                    .andExpect(status().isOk());

            verify(logService).getLogs("ERROR", "AI", 50);
        }

        @Test
        @DisplayName("defaults limit to 200 when not specified")
        void defaultsLimitTo200() throws Exception {
            when(logService.getLogs(null, null, 200)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/logs")).andExpect(status().isOk());

            verify(logService).getLogs(null, null, 200);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/logs/ingest")
    class Ingest {

        @Test
        @DisplayName("200 OK, delegates to ingestExternal")
        void delegatesToIngestExternal() throws Exception {
            mockMvc.perform(post("/api/v1/admin/logs/ingest")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"level\":\"INFO\",\"message\":\"test\"}"))
                    .andExpect(status().isOk());

            verify(logService).ingestExternal(org.mockito.ArgumentMatchers.any(SystemLog.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/logs/stream — SSE")
    class Stream {

        @Test
        @DisplayName("delegates to logService.streamLogs and returns an SseEmitter")
        void delegatesToStreamLogs() throws Exception {
            when(logService.streamLogs()).thenReturn(new SseEmitter());

            mockMvc.perform(get("/api/v1/admin/logs/stream"))
                    .andExpect(status().isOk());

            verify(logService).streamLogs();
        }
    }
}
