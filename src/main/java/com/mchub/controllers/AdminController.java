package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.services.AdminService;
import com.mchub.services.impl.DatabaseMigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final DatabaseMigrationService migrationService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> stats = adminService.getAdminDashboardOverview();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllTransactions() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllTransactions()));
    }

    @GetMapping("/revenue-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getRevenueStats()));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAnalytics()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers()));
    }

    @GetMapping("/users/mcs")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllMCs() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllMCs()));
    }



    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserStatus(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.get("isActive");
        Boolean isVerified = body.get("isVerified");
        if (isActive == null || isVerified == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "isActive and isVerified are required");
        }
        UserResponseDTO dto = adminService.updateUserStatus(Objects.requireNonNull(id), isActive, isVerified);
        return ResponseEntity.ok(ApiResponse.success("Update successful", dto));
    }

    @PostMapping("/migrate-db")
    public ResponseEntity<ApiResponse<String>> migrateDb() {
        migrationService.migrateFromMcHub();
        return ResponseEntity.ok(ApiResponse.success("Database migration started/completed successfully"));
    }


}
