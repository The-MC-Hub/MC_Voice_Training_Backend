package com.mchub.controllers;

import com.mchub.dto.CommunityStatsDTO;
import com.mchub.dto.LeaderboardEntryDTO;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.services.CommunityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommunityController.class)
@ContextConfiguration(classes = {CommunityController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class CommunityControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CommunityService communityService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/community/stats — public")
    class GetStats {

        @Test
        @DisplayName("200 OK, delegates to getCommunityStats")
        void returnsStats() throws Exception {
            when(communityService.getCommunityStats()).thenReturn(CommunityStatsDTO.builder().totalUsers(10).build());

            mockMvc.perform(get("/api/v1/community/stats")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/community/leaderboard — page size cap")
    class GetLeaderboard {

        @Test
        @DisplayName("caps page size at 50 when a larger value is requested")
        void capsSizeAt50() throws Exception {
            when(communityService.getLeaderboard(eq("streak"), eq("all_time"), any()))
                    .thenReturn(new PageImpl<>(List.<LeaderboardEntryDTO>of(), PageRequest.of(0, 50), 0));

            mockMvc.perform(get("/api/v1/community/leaderboard").param("size", "500"))
                    .andExpect(status().isOk());

            verify(communityService).getLeaderboard(eq("streak"), eq("all_time"),
                    org.mockito.ArgumentMatchers.argThat(p -> p.getPageSize() == 50));
        }

        @Test
        @DisplayName("uses defaults type=streak, period=all_time, page=0, size=20")
        void usesDefaults() throws Exception {
            when(communityService.getLeaderboard(eq("streak"), eq("all_time"), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/community/leaderboard")).andExpect(status().isOk());

            verify(communityService).getLeaderboard(eq("streak"), eq("all_time"),
                    org.mockito.ArgumentMatchers.argThat(p -> p.getPageNumber() == 0 && p.getPageSize() == 20));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/community/leaderboard/me — requires auth")
    class GetMyRank {

        @Test
        @DisplayName("uses caller's userId from SecurityContext")
        void usesCallerUserId() throws Exception {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("user-1", null, List.of()));
            when(communityService.getUserRank("user-1", "streak", "all_time")).thenReturn(null);

            mockMvc.perform(get("/api/v1/community/leaderboard/me")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/community/active-arenas — works for both guest and authenticated")
    class GetActiveArenas {

        @Test
        @DisplayName("passes null userId when unauthenticated (no exception propagates)")
        void passesNullUserIdWhenUnauthenticated() throws Exception {
            when(communityService.getActiveArenas(null)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/community/active-arenas")).andExpect(status().isOk());

            verify(communityService).getActiveArenas(null);
        }

        @Test
        @DisplayName("passes caller's userId when authenticated")
        void passesCallerUserIdWhenAuthenticated() throws Exception {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("user-1", null, List.of()));
            when(communityService.getActiveArenas("user-1")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/community/active-arenas")).andExpect(status().isOk());

            verify(communityService).getActiveArenas("user-1");
        }
    }
}
