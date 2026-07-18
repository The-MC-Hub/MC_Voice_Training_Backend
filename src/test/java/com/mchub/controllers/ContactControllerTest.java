package com.mchub.controllers;

import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.services.EmailService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContactController.class)
@ContextConfiguration(classes = {ContactController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class ContactControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private EmailService emailService;

    @Nested
    @DisplayName("POST /api/v1/public/contact")
    class SendContact {

        @Test
        @DisplayName("400 VALIDATION_FAILED when name is empty")
        void rejectsEmptyName() throws Exception {
            mockMvc.perform(post("/api/v1/public/contact")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\",\"email\":\"a@test.local\",\"message\":\"hi\"}"))
                    .andExpect(status().isBadRequest());

            verify(emailService, never()).sendSimpleEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED for a malformed email")
        void rejectsInvalidEmail() throws Exception {
            mockMvc.perform(post("/api/v1/public/contact")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"email\":\"not-an-email\",\"message\":\"hi\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED when message exceeds 2000 characters")
        void rejectsOverlongMessage() throws Exception {
            String longMessage = "a".repeat(2001);
            mockMvc.perform(post("/api/v1/public/contact")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"email\":\"a@test.local\",\"message\":\"" + longMessage + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("200 OK and sends email with valid input")
        void sendsEmailOnValidInput() throws Exception {
            mockMvc.perform(post("/api/v1/public/contact")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"QA Tester\",\"email\":\"qa@test.local\",\"message\":\"Hello\"}"))
                    .andExpect(status().isOk());

            verify(emailService).sendSimpleEmail(anyString(), anyString(), anyString());
        }
    }
}
