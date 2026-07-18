package com.mchub.controllers;

import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.MCProfile;
import com.mchub.services.MCProfileService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MCController.class)
@ContextConfiguration(classes = {MCController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class MCControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MCProfileService mcProfileService;

    private static final String USER_ID = "user-mc-001";

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
    @DisplayName("GET /api/v1/mcs/dashboard")
    class GetDashboard {

        @Test
        @DisplayName("200 OK, delegates using caller's userId")
        void returnsDashboardForCaller() throws Exception {
            when(mcProfileService.getDashboardStats(USER_ID)).thenReturn(Map.of("totalPractices", 5L));

            mockMvc.perform(get("/api/v1/mcs/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalPractices").value(5));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/mcs/profile")
    class UpdateProfile {

        @Test
        @DisplayName("200 OK, delegates to mcProfileService with caller's userId")
        void updatesProfileForCaller() throws Exception {
            when(mcProfileService.updateProfile(eq(USER_ID), any(MCProfile.class)))
                    .thenReturn(MCProfile.builder().user(USER_ID).biography("New bio").build());

            mockMvc.perform(put("/api/v1/mcs/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"biography\":\"New bio\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.biography").value("New bio"));
        }
    }
}
