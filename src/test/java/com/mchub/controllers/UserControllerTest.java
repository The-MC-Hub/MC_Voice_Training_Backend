package com.mchub.controllers;

import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.UserStats;
import com.mchub.services.GamificationService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression guard for the audit finding (Remaining_Modules_Audit_Report.md
 * 3.7, naming issue): POST /me/streak/freeze does NOT actually consume a
 * freeze — it only re-displays the current streak snapshot. The freeze is
 * consumed elsewhere, inside GamificationServiceImpl.processLoginStreak()
 * when a 2-day login gap is detected. Tests here confirm the endpoint's
 * TRUE (read-only) behavior, not the name's implication.
 */
@WebMvcTest(controllers = UserController.class)
@ContextConfiguration(classes = {UserController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private GamificationService gamificationService;

    private static final String USER_ID = "user-streak-001";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserStats.UserStatsBuilder baseStats() {
        return UserStats.builder().userId(USER_ID).loginStreak(0).longestLoginStreak(0).freezesAvailable(1);
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/streak — frame resolution")
    class GetLoginStreak {

        @Test
        @DisplayName("streak=0 resolves to frame NONE, next frame SPARK")
        void resolvesNoneFrameAtZero() throws Exception {
            when(gamificationService.getOrCreateUserStats(USER_ID)).thenReturn(baseStats().loginStreak(0).build());

            mockMvc.perform(get("/api/v1/users/me/streak"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.streakFrame").value("NONE"))
                    .andExpect(jsonPath("$.data.nextFrame").value("SPARK"))
                    .andExpect(jsonPath("$.data.daysToNextFrame").value(3));
        }

        @Test
        @DisplayName("streak=100+ resolves to IMMORTAL with no next frame")
        void resolvesImmortalAtHundredPlus() throws Exception {
            when(gamificationService.getOrCreateUserStats(USER_ID)).thenReturn(baseStats().loginStreak(150).build());

            mockMvc.perform(get("/api/v1/users/me/streak"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.streakFrame").value("IMMORTAL"))
                    .andExpect(jsonPath("$.data.nextFrame").doesNotExist())
                    .andExpect(jsonPath("$.data.daysToNextFrame").value(0));
        }

        @Test
        @DisplayName("streak=7 (FLAME boundary) resolves to FLAME, next STORM in 7 days")
        void resolvesFlameAtSevenDayBoundary() throws Exception {
            when(gamificationService.getOrCreateUserStats(USER_ID)).thenReturn(baseStats().loginStreak(7).build());

            mockMvc.perform(get("/api/v1/users/me/streak"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.streakFrame").value("FLAME"))
                    .andExpect(jsonPath("$.data.nextFrame").value("STORM"))
                    .andExpect(jsonPath("$.data.daysToNextFrame").value(7));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/me/streak/freeze — REGRESSION GUARD: read-only, misleading name")
    class UseFreeze {

        @Test
        @DisplayName("400 with fail envelope when no freezes available — does not call processLoginStreak")
        void returns400WhenNoFreezesAvailable() throws Exception {
            when(gamificationService.getOrCreateUserStats(USER_ID)).thenReturn(baseStats().freezesAvailable(0).build());

            mockMvc.perform(post("/api/v1/users/me/streak/freeze"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("fail"));

            org.mockito.Mockito.verify(gamificationService, org.mockito.Mockito.never()).processLoginStreak(USER_ID);
        }

        @Test
        @DisplayName("200 OK just re-returns the current streak snapshot — does NOT decrement freezesAvailable itself")
        void returnsCurrentSnapshotWithoutConsumingFreeze() throws Exception {
            UserStats stats = baseStats().freezesAvailable(1).loginStreak(5).build();
            when(gamificationService.getOrCreateUserStats(USER_ID)).thenReturn(stats);

            mockMvc.perform(post("/api/v1/users/me/streak/freeze"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.freezesAvailable").value(1)); // unchanged — this endpoint never consumes it

            org.mockito.Mockito.verify(gamificationService, org.mockito.Mockito.never()).processLoginStreak(USER_ID);
        }
    }
}
