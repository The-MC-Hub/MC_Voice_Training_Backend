package com.mchub.controllers;

import com.mchub.dto.MinigameResultDTO;
import com.mchub.dto.MinigameSubmitRequest;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.services.MinigameService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MinigameController.class)
@ContextConfiguration(classes = {MinigameController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class MinigameControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MinigameService minigameService;

    private static final String USER_ID = "user-mini-001";

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
    @DisplayName("GET /api/v1/minigames/speed-reader/prompts — public")
    class GetPrompts {

        @Test
        @DisplayName("defaults to NORMAL/8 rounds when not specified")
        void usesDefaultsWhenNotSpecified() throws Exception {
            when(minigameService.getSpeedReaderPrompts("NORMAL", 8)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/minigames/speed-reader/prompts")).andExpect(status().isOk());

            verify(minigameService).getSpeedReaderPrompts("NORMAL", 8);
        }

        @Test
        @DisplayName("forwards custom difficulty/rounds params")
        void forwardsCustomParams() throws Exception {
            when(minigameService.getSpeedReaderPrompts("HARD", 3)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/minigames/speed-reader/prompts")
                            .param("difficulty", "HARD").param("rounds", "3"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/minigames/speed-reader/submit")
    class SubmitRun {

        @Test
        @DisplayName("400 when difficulty is missing — bean validation")
        void rejectsMissingDifficulty() throws Exception {
            mockMvc.perform(post("/api/v1/minigames/speed-reader/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"roundsCleared\":5,\"bestCombo\":3}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("200 OK, submits using caller's userId from SecurityContext")
        void submitsWithCallerUserId() throws Exception {
            when(minigameService.submitSpeedReaderRun(eq(USER_ID), any(MinigameSubmitRequest.class)))
                    .thenReturn(MinigameResultDTO.builder().score(560).build());

            mockMvc.perform(post("/api/v1/minigames/speed-reader/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"difficulty\":\"EASY\",\"roundsCleared\":5,\"bestCombo\":3}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/minigames/leaderboard — public")
    class GetLeaderboard {

        @Test
        @DisplayName("defaults to SPEED_READER/20 when not specified")
        void usesDefaultsWhenNotSpecified() throws Exception {
            when(minigameService.getLeaderboard("SPEED_READER", 20)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/minigames/leaderboard")).andExpect(status().isOk());
        }
    }
}
