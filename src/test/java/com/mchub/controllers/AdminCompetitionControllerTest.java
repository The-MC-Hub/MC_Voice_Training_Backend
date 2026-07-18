package com.mchub.controllers;

import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.Competition;
import com.mchub.services.CompetitionService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCompetitionController.class)
@ContextConfiguration(classes = {AdminCompetitionController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminCompetitionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CompetitionService competitionService;

    @Nested
    @DisplayName("GET /api/v1/admin/competitions")
    class GetAll {

        @Test
        @DisplayName("200 OK, delegates to getAllCompetitions")
        void returnsAll() throws Exception {
            when(competitionService.getAllCompetitions()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/competitions")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/competitions")
    class Create {

        @Test
        @DisplayName("200 OK, delegates to createCompetition")
        void createsCompetition() throws Exception {
            when(competitionService.createCompetition(any(Competition.class)))
                    .thenReturn(Competition.builder().id("comp-1").title("Weekly").build());

            mockMvc.perform(post("/api/v1/admin/competitions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Weekly\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/competitions/{id} — regression guard for audit fix 2.5")
    class Delete {

        @Test
        @DisplayName("404 RESOURCE_NOT_FOUND when competition doesn't exist — not a silent 200")
        void returns404ForUnknownId() throws Exception {
            org.mockito.Mockito.doThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND))
                    .when(competitionService).deleteCompetition("missing");

            mockMvc.perform(delete("/api/v1/admin/competitions/{id}", "missing"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("200 OK when competition exists")
        void deletesExistingCompetition() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/competitions/{id}", "comp-1"))
                    .andExpect(status().isOk());

            verify(competitionService).deleteCompetition("comp-1");
        }
    }
}
