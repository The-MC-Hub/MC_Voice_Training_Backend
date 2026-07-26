package com.mchub.controllers;

import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.UserHighlight;
import com.mchub.services.UserHighlightService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserHighlightController.class)
@ContextConfiguration(classes = {UserHighlightController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class UserHighlightControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private UserHighlightService highlightService;

    private static final String OWNER_ID = "owner-001";
    private static final String OTHER_USER_ID = "other-user-001";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/highlights/reading-guides/{guideId} — DEFECT-001 fix: no userId in path")
    class GetHighlights {

        @Test
        @DisplayName("uses caller's userId from JWT, never a URL parameter")
        void usesCallerIdFromJwt() throws Exception {
            when(highlightService.getHighlights(OWNER_ID, "guide-1"))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/highlights/reading-guides/{guideId}", "guide-1"))
                    .andExpect(status().isOk());

            verify(highlightService).getHighlights(OWNER_ID, "guide-1");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/highlights — DEFECT-001 fix: userId forced from JWT")
    class CreateHighlight {

        @Test
        @DisplayName("ignores client-supplied userId and uses the JWT's userId instead")
        void ignoresClientSuppliedUserId() throws Exception {
            UserHighlight created = UserHighlight.builder().userId(OWNER_ID).noteContent("hi").build();
            when(highlightService.createHighlight(eq(OWNER_ID), any(UserHighlight.class))).thenReturn(created);

            mockMvc.perform(post("/api/v1/highlights")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"" + OTHER_USER_ID + "\",\"noteContent\":\"hi\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(OWNER_ID));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/highlights/{id} — ownership check")
    class UpdateHighlight {

        @Test
        @DisplayName("403 ACCESS_DENIED when highlight belongs to a different user")
        void rejectsUpdatingAnotherUsersHighlight() throws Exception {
            when(highlightService.updateHighlight(eq(OWNER_ID), eq("h1"), any(UserHighlight.class)))
                    .thenThrow(new AppException(ErrorCode.ACCESS_DENIED, "Access denied"));

            mockMvc.perform(put("/api/v1/highlights/{id}", "h1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"noteContent\":\"hacked\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 OK when caller owns the highlight")
        void allowsUpdatingOwnHighlight() throws Exception {
            UserHighlight own = UserHighlight.builder().id("h1").userId(OWNER_ID).build();
            when(highlightService.updateHighlight(eq(OWNER_ID), eq("h1"), any(UserHighlight.class))).thenReturn(own);

            mockMvc.perform(put("/api/v1/highlights/{id}", "h1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"noteContent\":\"updated\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("404 RESOURCE_NOT_FOUND for unknown id")
        void returns404ForUnknownId() throws Exception {
            when(highlightService.updateHighlight(eq(OWNER_ID), eq("missing"), any(UserHighlight.class)))
                    .thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Not found"));

            mockMvc.perform(put("/api/v1/highlights/{id}", "missing")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/highlights/{id} — ownership check")
    class DeleteHighlight {

        @Test
        @DisplayName("403 ACCESS_DENIED when highlight belongs to a different user")
        void rejectsDeletingAnotherUsersHighlight() throws Exception {
            doThrow(new AppException(ErrorCode.ACCESS_DENIED, "Access denied"))
                    .when(highlightService).deleteHighlight(OWNER_ID, "h1");

            mockMvc.perform(delete("/api/v1/highlights/{id}", "h1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 OK when caller owns the highlight")
        void allowsDeletingOwnHighlight() throws Exception {
            mockMvc.perform(delete("/api/v1/highlights/{id}", "h1")).andExpect(status().isOk());

            verify(highlightService).deleteHighlight(OWNER_ID, "h1");
        }
    }
}
