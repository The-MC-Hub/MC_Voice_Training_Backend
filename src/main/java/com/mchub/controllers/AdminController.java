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

import java.util.Map;

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

    @GetMapping("/growth-analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGrowthAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getGrowthAnalytics()));
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

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@RequestBody Map<String, String> body) {
        String name     = body.get("name");
        String email    = body.get("email");
        String password = body.get("password");
        String role     = body.getOrDefault("role", "CLIENT");
        if (name == null || email == null || password == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "name, email, password required");
        }
        UserResponseDTO dto = adminService.createUser(name, email, password, role);
        return ResponseEntity.ok(ApiResponse.success("User created", dto));
    }

    @PostMapping("/users/{id}/send-reset-email")
    public ResponseEntity<ApiResponse<Void>> sendResetEmail(@PathVariable String id) {
        adminService.sendPasswordResetEmail(id);
        return ResponseEntity.ok(ApiResponse.success("Reset email sent", null));
    }

    @PostMapping("/users/{id}/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "newPassword required");
        }
        adminService.changeUserPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password changed", null));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }

    @GetMapping("/users/{id}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserStats(id)));
    }

    @PostMapping("/users/{id}/notify-email")
    public ResponseEntity<ApiResponse<Void>> sendNotificationEmail(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        String subject = body.get("subject");
        String content = body.get("content");
        if (subject == null || content == null || subject.isBlank() || content.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "subject and content required");
        }
        adminService.sendNotificationEmail(id, subject, content);
        return ResponseEntity.ok(ApiResponse.success("Email sent", null));
    }

    @PostMapping("/migrate-db")
    public ResponseEntity<ApiResponse<String>> migrateDb() {
        migrationService.migrateFromMcHub();
        return ResponseEntity.ok(ApiResponse.success("Database migration started/completed successfully"));
    }


}
