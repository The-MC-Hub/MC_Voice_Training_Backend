package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.User;
import com.mchub.repositories.UserRepository;
import com.mchub.services.AuthService;
import com.mchub.services.AuditLogService;
import com.mchub.services.JwtService;
import com.mchub.mapper.UserMapper;
import com.mchub.enums.AuditAction;
import com.mchub.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mchub.repositories.ReferralRepository;
import com.mchub.services.GamificationService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;
    private final GamificationService gamificationService;

    private static final String REFERRAL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom REFERRAL_RANDOM = new SecureRandom();

    private String generateReferralCode() {
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) sb.append(REFERRAL_CHARS.charAt(REFERRAL_RANDOM.nextInt(REFERRAL_CHARS.length())));
        return sb.toString();
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @RequestBody @Valid RegisterRequest req,
            HttpServletRequest request) {
        var user = authService.register(Objects.requireNonNull(req));
        // Send OTP async — don't block response
        try { authService.sendOtp(user.getEmail()); } catch (Exception ignored) {}

        auditLogService.log(user.getId(), AuditAction.AUTH_REGISTER, "User", user.getId(),
                "{\"role\":\"" + req.getRole() + "\"}", request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. OTP sent to email.",
                        Map.of("requiresVerification", true, "email", user.getEmail())));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEmailByToken(@RequestParam String token) {
        AuthService.LoginResponse resp = authService.verifyEmailByToken(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully",
                Map.of("token", resp.token(), "user", userMapper.toResponseDTO(resp.user()))));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        authService.verifyOtp(Objects.requireNonNull(email), Objects.requireNonNull(code));
        // After verify, auto-login
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully",
                Map.of("token", token, "user", userMapper.toResponseDTO(user))));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestBody Map<String, String> body) {
        authService.resendOtp(Objects.requireNonNull(body.get("email")));
        return ResponseEntity.ok(ApiResponse.success("OTP resent", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @RequestBody @Valid LoginRequest req,
            HttpServletRequest request) {
        try {
            AuthService.LoginResponse resp = authService.login(Objects.requireNonNull(req.getEmail()), Objects.requireNonNull(req.getPassword()));
            auditLogService.log(resp.user().getId(), AuditAction.AUTH_LOGIN,
                    "User", resp.user().getId(), null, request);
            try { gamificationService.processLoginStreak(resp.user().getId()); } catch (Exception ignored) {}
            return ResponseEntity.ok(ApiResponse.success("Login successful",
                    Map.of("token", resp.token(), "user", userMapper.toResponseDTO(resp.user()))));
        } catch (AppException ex) {
            if (ex.getErrorCode() == ErrorCode.ADMIN_OTP_REQUIRED) {
                String adminEmail = ex.getMessage().replace("ADMIN_OTP_REQUIRED:", "");
                return ResponseEntity.status(202).body(ApiResponse.success("ADMIN_OTP_REQUIRED",
                        Map.of("requiresAdminOtp", true, "email", adminEmail)));
            }
            auditLogService.logError(null, AuditAction.AUTH_LOGIN_FAILED, "User", "email=" + req.getEmail() + " " + SecurityUtils.safeMessage(ex), request);
            throw ex;
        } catch (Exception ex) {
            auditLogService.logError(null, AuditAction.AUTH_LOGIN_FAILED, "User", "email=" + req.getEmail() + " " + SecurityUtils.safeMessage(ex), request);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }
    }



    @PostMapping("/google")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginWithGoogle(
            @RequestBody @Valid GoogleLoginRequest req,
            HttpServletRequest request) {
        try {
            var result = authService.loginWithGoogle(req.getIdToken());
            if (result instanceof AuthService.GoogleAuthResult.LoggedIn loggedIn) {
                var resp = loggedIn.response();
                auditLogService.log(resp.user().getId(), AuditAction.AUTH_LOGIN, "User", resp.user().getId(),
                        "{\"method\":\"google\"}", request);
                try { gamificationService.processLoginStreak(resp.user().getId()); } catch (Exception ignored) {}
                return ResponseEntity.ok(ApiResponse.success("Login successful",
                        Map.of("token", resp.token(), "user", userMapper.toResponseDTO(resp.user()))));
            }
            var pending = (AuthService.GoogleAuthResult.PendingRegistration) result;
            return ResponseEntity.status(202).body(ApiResponse.success("Choose a role to finish signing up",
                    Map.of("requiresRoleSelection", true,
                            "pendingToken", pending.pendingToken(),
                            "email", pending.email(),
                            "name", pending.name() != null ? pending.name() : "")));
        } catch (AppException ex) {
            if (ex.getErrorCode() == ErrorCode.ADMIN_OTP_REQUIRED) {
                String adminEmail = ex.getMessage().replace("ADMIN_OTP_REQUIRED:", "");
                return ResponseEntity.status(202).body(ApiResponse.success("ADMIN_OTP_REQUIRED",
                        Map.of("requiresAdminOtp", true, "email", adminEmail)));
            }
            auditLogService.logError(null, AuditAction.AUTH_LOGIN_FAILED, "User", SecurityUtils.safeMessage(ex), request);
            throw ex;
        }
    }

    @PostMapping("/google/complete-registration")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeGoogleRegistration(
            @RequestBody @Valid CompleteGoogleRegistrationRequest req,
            HttpServletRequest request) {
        AuthService.LoginResponse resp = authService.completeGoogleRegistration(
                req.getPendingToken(), req.getRole(), req.getReferralCode());
        auditLogService.log(resp.user().getId(), AuditAction.AUTH_REGISTER, "User", resp.user().getId(),
                "{\"method\":\"google\"}", request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Registration successful",
                Map.of("token", resp.token(), "user", userMapper.toResponseDTO(resp.user()))));
    }

    @PostMapping("/verify-admin-login-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyAdminLoginOtp(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String email = body.get("email");
        String code = body.get("code");
        AuthService.LoginResponse resp = authService.verifyAdminLoginOtp(
                Objects.requireNonNull(email), Objects.requireNonNull(code));
        auditLogService.log(resp.user().getId(), AuditAction.ADMIN_LOGIN_OTP_VERIFY,
                "User", resp.user().getId(), "{\"method\":\"admin-otp\"}", request);
        return ResponseEntity.ok(ApiResponse.success("Admin login verified",
                Map.of("token", resp.token(), "user", userMapper.toResponseDTO(resp.user()))));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody Map<String, String> body) {
        authService.forgotPassword(body.get("email"));
        return ResponseEntity.ok(ApiResponse.success("Reset code sent to your email", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("email"), body.get("code"), body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMe() {
        String userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));
        return ResponseEntity.ok(ApiResponse.success("User retrieved", Map.of("user", userMapper.toResponseDTO(user))));
    }

    @PostMapping("/referral-code/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateReferralCodeEndpoint() {
        String userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));
        if (user.getReferralCode() != null) {
            return ResponseEntity.ok(ApiResponse.success("Referral code retrieved",
                    Map.of("referralCode", user.getReferralCode())));
        }
        String code = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            String candidate = generateReferralCode();
            if (userRepository.findByReferralCode(candidate).isEmpty()) {
                code = candidate;
                break;
            }
        }
        if (code == null) code = generateReferralCode();
        user.setReferralCode(code);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Referral code generated",
                Map.of("referralCode", code)));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(@RequestBody Map<String, Object> settings) {
        String userId = SecurityUtils.getCurrentUserId();
        var user = authService.updateSettings(userId, settings);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", 
                Map.of("user", userMapper.toResponseDTO(user))));
    }
}
