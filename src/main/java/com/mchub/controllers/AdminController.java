package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.enums.AuditAction;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Booking;
import com.mchub.services.AdminService;
import com.mchub.services.AuditLogService;
import com.mchub.services.BookingService;
import com.mchub.services.impl.DatabaseMigrationService;
import com.mchub.models.SystemSetting;
import com.mchub.models.User;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.SystemSettingRepository;
import com.mchub.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
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
    private final BookingService bookingService;
    private final DatabaseMigrationService migrationService;
    private final AuditLogService auditLogService;
    private final SystemSettingRepository systemSettingRepo;
    private final UserRepository userRepository;

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

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserById(id)));
    }

    @GetMapping("/users/mcs")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllMCs() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllMCs()));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserStatus(@PathVariable String id,
            @RequestBody Map<String, Boolean> body, HttpServletRequest request) {
        Boolean isActive = body.get("isActive");
        Boolean isVerified = body.get("isVerified");
        if (isActive == null || isVerified == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "isActive and isVerified are required");
        }
        UserResponseDTO dto = adminService.updateUserStatus(Objects.requireNonNull(id), isActive, isVerified);
        auditLogService.log(SecurityUtils.getCurrentUserId(), AuditAction.ADMIN_UPDATE_USER_STATUS, "User", id,
                "{\"isActive\":" + isActive + ",\"isVerified\":" + isVerified + "}", request);
        return ResponseEntity.ok(ApiResponse.success("Update successful", dto));
    }

    @PutMapping("/users/{id}/plan")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserPlan(@PathVariable String id,
            @RequestBody Map<String, String> body, HttpServletRequest request) {
        String plan = body.get("plan");
        if (plan == null || plan.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Plan is required");
        }
        UserResponseDTO dto = adminService.updateUserPlan(id, plan);
        auditLogService.log(SecurityUtils.getCurrentUserId(), AuditAction.ADMIN_UPDATE_USER_STATUS, "User", id,
                "{\"newPlan\":\"" + plan + "\"}", request);
        return ResponseEntity.ok(ApiResponse.success("Plan updated successfully", dto));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        String password = (String) body.get("password");
        String role = body.getOrDefault("role", "CLIENT").toString();
        String phoneNumber = (String) body.get("phoneNumber");
        String adminNote = (String) body.get("adminNote");
        String planStr = (String) body.get("plan");
        String couponId = (String) body.get("couponId");
        if (name == null || email == null || password == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "name, email, password required");
        }
        UserResponseDTO dto = adminService.createUser(name, email, password, role, phoneNumber, adminNote, planStr,
                couponId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), AuditAction.ADMIN_CREATE_USER, "User", dto.getId(),
                "{\"email\":\"" + email + "\",\"role\":\"" + role + "\"}", request);
        return ResponseEntity.ok(ApiResponse.success("User created", dto));
    }

    @PostMapping("/users/{id}/send-reset-email")
    public ResponseEntity<ApiResponse<Void>> sendResetEmail(@PathVariable String id, HttpServletRequest request) {
        adminService.sendPasswordResetEmail(id);
        auditLogService.log(SecurityUtils.getCurrentUserId(), AuditAction.ADMIN_SEND_RESET_EMAIL, "User", id, null,
                request);
        return ResponseEntity.ok(ApiResponse.success("Reset email sent", null));
    }

    @PostMapping("/users/{id}/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@PathVariable String id,
            @RequestBody Map<String, String> body, HttpServletRequest request) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "newPassword required");
        }
        adminService.changeUserPassword(id, newPassword);
        auditLogService.log(SecurityUtils.getCurrentUserId(), AuditAction.ADMIN_CHANGE_PASSWORD, "User", id, null,
                request);
        return ResponseEntity.ok(ApiResponse.success("Password changed", null));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id, HttpServletRequest request) {
        adminService.deleteUser(id);
        auditLogService.log(SecurityUtils.getCurrentUserId(), AuditAction.ADMIN_DELETE_USER, "User", id, null, request);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }

    @GetMapping("/users/{id}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserStats(id)));
    }

    @PostMapping("/users/{id}/notify-email")
    public ResponseEntity<ApiResponse<Void>> sendNotificationEmail(@PathVariable String id,
            @RequestBody Map<String, String> body, HttpServletRequest request) {
        String subject = body.get("subject");
        String content = body.get("content");
        if (subject == null || content == null || subject.isBlank() || content.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "subject and content required");
        }
        adminService.sendNotificationEmail(id, subject, content);
        auditLogService.log(SecurityUtils.getCurrentUserId(), AuditAction.ADMIN_NOTIFY_EMAIL, "User", id,
                "{\"subject\":\"" + subject + "\"}", request);
        return ResponseEntity.ok(ApiResponse.success("Email sent", null));
    }

    @PostMapping("/migrate-db")
    public ResponseEntity<ApiResponse<String>> migrateDb(HttpServletRequest request) {
        migrationService.migrateFromMcHub();
        auditLogService.log(SecurityUtils.getCurrentUserId(), AuditAction.ADMIN_MIGRATE_DB, "System", null, null,
                request);
        return ResponseEntity.ok(ApiResponse.success("Database migration started/completed successfully"));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        List<Map<String, Object>> enriched = bookings.stream()
                .map(b -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("_id", b.getId());
                    m.put("client", b.getClient());
                    m.put("mc", b.getMc());
                    m.put("eventName", b.getEventName());
                    m.put("eventDate", b.getEventDate());
                    m.put("startTime", b.getStartTime());
                    m.put("endTime", b.getEndTime());
                    m.put("location", b.getLocation());
                    m.put("eventType", b.getEventType());
                    m.put("description", b.getDescription());
                    m.put("audienceSize", b.getAudienceSize());
                    m.put("budget", b.getBudget());
                    m.put("price", b.getPrice());
                    m.put("status", b.getStatus());
                    m.put("paymentStatus", b.getPaymentStatus());
                    m.put("rejectionReason", b.getRejectionReason());
                    m.put("couponCode", b.getCouponCode());
                    m.put("createdAt", b.getCreatedAt());
                    m.put("decidedAt", b.getDecidedAt());

                    // Enrich with user names
                    userRepository.findById(b.getClient()).ifPresent(u -> {
                        m.put("clientName", u.getName());
                        m.put("clientAvatar", u.getAvatar());
                    });
                    userRepository.findById(b.getMc()).ifPresent(u -> {
                        m.put("mcName", u.getName());
                        m.put("mcAvatar", u.getAvatar());
                    });

                    return m;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(enriched));
    }

    @GetMapping("/settings/guest-cooldown")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGuestCooldown() {
        int hours = systemSettingRepo.findById("GUEST_COOLDOWN_HOURS").map(s -> {
            try {
                return Integer.parseInt(s.getValue());
            } catch (Exception e) {
                return 3;
            }
        }).orElse(3);
        return ResponseEntity.ok(ApiResponse.success("OK", Map.of("hours", hours)));
    }

    @PutMapping("/settings/guest-cooldown")
    public ResponseEntity<ApiResponse<Void>> setGuestCooldown(@RequestParam int hours) {
        if (hours < 1 || hours > 168)
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Giờ phải từ 1 đến 168");
        SystemSetting s = systemSettingRepo.findById("GUEST_COOLDOWN_HOURS").orElse(new SystemSetting());
        s.setKey("GUEST_COOLDOWN_HOURS");
        s.setValue(String.valueOf(hours));
        systemSettingRepo.save(s);
        return ResponseEntity.ok(ApiResponse.success("Guest cooldown updated to " + hours + " hours", null));
    }
}
