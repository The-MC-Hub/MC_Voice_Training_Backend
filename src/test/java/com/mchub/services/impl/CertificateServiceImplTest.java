package com.mchub.services.impl;

import com.mchub.dto.CreateCertificateRequest;
import com.mchub.models.Certificate;
import com.mchub.repositories.CertificateRepository;
import com.mchub.repositories.MCProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CertificateServiceImpl. This service is deprecated per its
 * class javadoc — manual MC certificates were replaced by auto-issued course
 * certificates. Only getCertificatesByMCProfile() still functions (repurposed
 * as a userId lookup for backward-compat display); the other 3 methods are
 * intentional UnsupportedOperationException stubs kept only so the old
 * CertificateController still compiles. Tests confirm both halves of that
 * contract.
 */
@ExtendWith(MockitoExtension.class)
class CertificateServiceImplTest {

    @Mock private CertificateRepository certificateRepository;
    @Mock private MCProfileRepository mcProfileRepository;

    private CertificateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CertificateServiceImpl(certificateRepository, mcProfileRepository);
    }

    @Nested
    @DisplayName("deprecated stub methods — must throw UnsupportedOperationException")
    class DeprecatedStubs {

        @Test
        @DisplayName("addCertificate always throws")
        void addCertificateThrows() {
            assertThatThrownBy(() -> service.addCertificate("mc-1", new CreateCertificateRequest()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("verifyCertificate always throws")
        void verifyCertificateThrows() {
            assertThatThrownBy(() -> service.verifyCertificate("cert-1", "admin-1"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("deleteCertificate always throws")
        void deleteCertificateThrows() {
            assertThatThrownBy(() -> service.deleteCertificate("cert-1", "mc-1"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("getCertificatesByMCProfile — still functional, repurposed as userId lookup")
    class GetCertificatesByMCProfile {

        @Test
        @DisplayName("delegates to findByUserId, treating the mcProfileId param as a userId")
        void delegatesToFindByUserId() {
            Certificate cert = Certificate.builder().id("cert-1").userId("user-1").build();
            when(certificateRepository.findByUserId("user-1")).thenReturn(List.of(cert));

            List<Certificate> result = service.getCertificatesByMCProfile("user-1");

            assertThat(result).containsExactly(cert);
        }

        @Test
        @DisplayName("returns empty list when user has no certificates")
        void returnsEmptyWhenNoCertificates() {
            when(certificateRepository.findByUserId("user-none")).thenReturn(List.of());

            List<Certificate> result = service.getCertificatesByMCProfile("user-none");

            assertThat(result).isEmpty();
        }
    }
}
