package com.mchub.services;

import com.mchub.dto.CreateCertificateRequest;
import com.mchub.models.Certificate;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface CertificateService {

        @PreAuthorize("hasAuthority('MC')")
    Certificate addCertificate(String mcProfileId, CreateCertificateRequest req);

        @PreAuthorize("hasAuthority('ADMIN')")
    Certificate verifyCertificate(String certId, String adminId);

        List<Certificate> getCertificatesByMCProfile(String mcProfileId);

    @PreAuthorize("hasAuthority('MC')")
    void deleteCertificate(String certId, String mcProfileId);
}
