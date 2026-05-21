package com.mchub.services.impl;

import com.mchub.dto.CreateCertificateRequest;
import com.mchub.models.Certificate;
import com.mchub.repositories.CertificateRepository;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.services.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Manual MC certificate system is deprecated.
 * Certificates are now auto-issued on course quiz completion via CourseService.
 * These stubs keep the old CertificateController compiling during migration.
 */
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final MCProfileRepository mcProfileRepository;

    @Override
    public Certificate addCertificate(String mcProfileId, CreateCertificateRequest req) {
        throw new UnsupportedOperationException(
                "Manual certificates are deprecated. Certificates are issued automatically upon course completion.");
    }

    @Override
    public Certificate verifyCertificate(String certId, String adminId) {
        throw new UnsupportedOperationException(
                "Certificate verification is deprecated. Course certificates are auto-verified.");
    }

    @Override
    public List<Certificate> getCertificatesByMCProfile(String mcProfileId) {
        // mcProfileId is now treated as userId for backward-compat display
        return certificateRepository.findByUserId(mcProfileId);
    }

    @Override
    public void deleteCertificate(String certId, String mcProfileId) {
        throw new UnsupportedOperationException(
                "Certificate deletion is deprecated. Course certificates are permanent.");
    }
}
