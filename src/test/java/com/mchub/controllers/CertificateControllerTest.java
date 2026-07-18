package com.mchub.controllers;

import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.mapper.CertificateMapper;
import com.mchub.models.Certificate;
import com.mchub.models.User;
import com.mchub.repositories.UserRepository;
import com.mchub.services.CertificateService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CertificateService is deprecated (per its class javadoc) — addCertificate/
 * verifyCertificate/deleteCertificate always throw UnsupportedOperationException,
 * which GlobalExceptionHandler maps to HTTP 410 Gone. Only the GET
 * (getCertificatesByMCProfile, repurposed as a userId lookup) still works.
 */
@WebMvcTest(controllers = CertificateController.class)
@ContextConfiguration(classes = {CertificateController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class CertificateControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CertificateService certificateService;
    @MockBean private UserRepository userRepository;
    @MockBean private CertificateMapper certificateMapper;

    private static final String USER_ID = "user-cert-001";

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
    @DisplayName("POST /api/v1/certificates — deprecated, always errors")
    class AddCertificate {

        @Test
        @DisplayName("404 MC_PROFILE_NOT_FOUND when caller has no MC profile")
        void returns404WhenNoMcProfile() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));

            mockMvc.perform(post("/api/v1/certificates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Cert\",\"issuer\":\"Issuer\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("410 Gone when service throws UnsupportedOperationException (deprecated feature)")
        void returns410ForDeprecatedFeature() throws Exception {
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(User.builder().id(USER_ID).mcProfile("mc-1").build()));
            when(certificateService.addCertificate(org.mockito.ArgumentMatchers.eq("mc-1"), any()))
                    .thenThrow(new UnsupportedOperationException("deprecated"));

            mockMvc.perform(post("/api/v1/certificates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Cert\",\"issuer\":\"Issuer\"}"))
                    .andExpect(status().isGone());
        }

        @Test
        @DisplayName("400 when name is blank — bean validation")
        void rejectsBlankName() throws Exception {
            mockMvc.perform(post("/api/v1/certificates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\",\"issuer\":\"Issuer\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/certificates/mc/{mcProfileId} — public, still functional")
    class GetCertificates {

        @Test
        @DisplayName("200 OK, maps entities through CertificateMapper")
        void returnsMappedCertificates() throws Exception {
            when(certificateService.getCertificatesByMCProfile("mc-1")).thenReturn(List.of(new Certificate()));

            mockMvc.perform(get("/api/v1/certificates/mc/{mcProfileId}", "mc-1")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/certificates/{id}/verify — deprecated, always errors")
    class Verify {

        @Test
        @DisplayName("410 Gone when service throws UnsupportedOperationException")
        void returns410ForDeprecatedFeature() throws Exception {
            when(certificateService.verifyCertificate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                    .thenThrow(new UnsupportedOperationException("deprecated"));

            mockMvc.perform(put("/api/v1/certificates/{id}/verify", "cert-1"))
                    .andExpect(status().isGone());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/certificates/{id} — deprecated, always errors")
    class DeleteCertificate {

        @Test
        @DisplayName("404 MC_PROFILE_NOT_FOUND when caller has no MC profile")
        void returns404WhenNoMcProfile() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));

            mockMvc.perform(delete("/api/v1/certificates/{id}", "cert-1")).andExpect(status().isNotFound());
        }
    }
}
