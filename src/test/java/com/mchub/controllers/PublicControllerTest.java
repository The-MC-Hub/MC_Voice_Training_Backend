package com.mchub.controllers;

import com.mchub.dto.MCProfileResponseDTO;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.services.PublicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicController.class)
@ContextConfiguration(classes = {PublicController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class PublicControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PublicService publicService;

    @Nested
    @DisplayName("GET /api/v1/public/landing, /featured-training — public")
    class LandingAndFeatured {

        @Test
        @DisplayName("landing delegates to getLandingData")
        void returnsLandingData() throws Exception {
            when(publicService.getLandingData()).thenReturn(Map.of("stats", Map.of()));

            mockMvc.perform(get("/api/v1/public/landing")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("featured-training delegates to getFeaturedMCTrainingStats")
        void returnsFeaturedStats() throws Exception {
            when(publicService.getFeaturedMCTrainingStats()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/public/featured-training")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/public/mcs/{id}")
    class GetMcProfile {

        @Test
        @DisplayName("404 MC_PROFILE_NOT_FOUND when service returns null")
        void returns404WhenProfileNull() throws Exception {
            when(publicService.getMCProfile("missing")).thenReturn(null);

            mockMvc.perform(get("/api/v1/public/mcs/{id}", "missing")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("200 OK wraps profile in a 'profile' key")
        void returnsProfileWrapped() throws Exception {
            MCProfileResponseDTO dto = new MCProfileResponseDTO();
            when(publicService.getMCProfile("mc-1")).thenReturn(dto);

            mockMvc.perform(get("/api/v1/public/mcs/{id}", "mc-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.profile").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/public/mcs — search results wrapper")
    class DiscoverMcs {

        @Test
        @DisplayName("wraps list with 'mcs' and 'results' count")
        void wrapsListWithCount() throws Exception {
            when(publicService.discoverMCs(any())).thenReturn(List.of(new MCProfileResponseDTO(), new MCProfileResponseDTO()));

            mockMvc.perform(get("/api/v1/public/mcs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.results").value(2));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/public/enums/*")
    class Enums {

        @Test
        @DisplayName("user-roles delegates to getUserRoles")
        void returnsUserRoles() throws Exception {
            when(publicService.getUserRoles()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/public/enums/user-roles")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("report-reasons delegates to getReportReasons")
        void returnsReportReasons() throws Exception {
            when(publicService.getReportReasons()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/public/enums/report-reasons")).andExpect(status().isOk());
        }
    }
}
