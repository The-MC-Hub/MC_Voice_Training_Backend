package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Certificate;
import com.mchub.repositories.UserRepository;
import com.mchub.services.CertificateService;
import com.mchub.mapper.CertificateMapper;
import com.mchub.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    private final UserRepository userRepository;
    private final CertificateMapper certificateMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<CertificateResponseDTO>> addCertificate(
            @RequestBody @Valid CreateCertificateRequest req) {
        String userId = SecurityUtils.getCurrentUserId();
        String mcProfileId = userRepository.findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
            .getMcProfile();
        if (mcProfileId == null || mcProfileId.isBlank()) {
            throw new AppException(ErrorCode.MC_PROFILE_NOT_FOUND, "You need to have an MC profile before adding a certificate");
        }
        Certificate cert = certificateService.addCertificate(mcProfileId, Objects.requireNonNull(req));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Certificate added successfully", certificateMapper.toResponseDTO(cert)));
    }

    @GetMapping("/mc/{mcProfileId}")
    public ResponseEntity<ApiResponse<List<CertificateResponseDTO>>> getCertificates(
            @PathVariable String mcProfileId) {
        List<CertificateResponseDTO> dtos = certificateService
            .getCertificatesByMCProfile(Objects.requireNonNull(mcProfileId))
            .stream().map(certificateMapper::toResponseDTO).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PutMapping("/{id}/verify")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CertificateResponseDTO>> verify(@PathVariable String id) {
        String adminId = SecurityUtils.getCurrentUserId();
        Certificate verified = certificateService.verifyCertificate(Objects.requireNonNull(id), adminId);
        return ResponseEntity.ok(ApiResponse.success("Verified successfully", certificateMapper.toResponseDTO(verified)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCertificate(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        String mcProfileId = userRepository.findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
            .getMcProfile();
        if (mcProfileId == null || mcProfileId.isBlank()) {
            throw new AppException(ErrorCode.MC_PROFILE_NOT_FOUND);
        }
        certificateService.deleteCertificate(Objects.requireNonNull(id), mcProfileId);
        return ResponseEntity.ok(ApiResponse.success("Deleted successfully", null));
    }
}
