package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.models.UserVoucher;
import com.mchub.repositories.UserVoucherRepository;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final UserVoucherRepository userVoucherRepository;

    // ================================================================
    //  GET /api/v1/vouchers/my  — all vouchers (active + used)
    // ================================================================
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserVoucher>>> getMyVouchers() {
        String userId = SecurityUtils.getCurrentUserId();
        List<UserVoucher> vouchers = userVoucherRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(ApiResponse.success("Vouchers retrieved", vouchers));
    }

    // ================================================================
    //  GET /api/v1/vouchers/my/available  — only unused, non-expired
    // ================================================================
    @GetMapping("/my/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserVoucher>>> getAvailableVouchers() {
        String userId = SecurityUtils.getCurrentUserId();
        List<UserVoucher> vouchers = userVoucherRepository
                .findByUserIdAndUsedAtIsNullAndActiveTrue(userId)
                .stream()
                .filter(v -> v.getExpiresAt() == null || v.getExpiresAt().isAfter(LocalDateTime.now()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Available vouchers retrieved", vouchers));
    }
}
