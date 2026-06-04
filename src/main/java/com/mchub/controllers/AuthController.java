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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @RequestBody @Valid RegisterRequest req,
            HttpServletRequest request) {
        var user = authService.register(Objects.requireNonNull(req));
        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        
        auditLogService.log(user.getId(), AuditAction.AUTH_REGISTER, "User", user.getId(),
                "{\"role\":\"" + req.getRole() + "\"}", request);
                
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", Map.of("token", token, "user", userMapper.toResponseDTO(user))));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @RequestBody @Valid LoginRequest req,
            HttpServletRequest request) {
        try {
            AuthService.LoginResponse resp = authService.login(Objects.requireNonNull(req.getEmail()), Objects.requireNonNull(req.getPassword()));
            


            auditLogService.log(resp.user().getId(), AuditAction.AUTH_LOGIN,
                    "User", resp.user().getId(), null, request);
            return ResponseEntity.ok(ApiResponse.success("Login successful",
                    Map.of("token", resp.token(), "user", userMapper.toResponseDTO(resp.user()))));
        } catch (AppException ex) {
            auditLogService.logError(null, AuditAction.AUTH_LOGIN, "User", ex.getMessage(), request);
            throw ex;
        } catch (Exception ex) {
            auditLogService.logError(null, AuditAction.AUTH_LOGIN, "User", ex.getMessage(), request);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, ex.getMessage());
        }
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

    @GetMapping("/fix-passwords")
    public ResponseEntity<?> fixPasswords() {
        authService.fixAllSeededPasswords();
        return ResponseEntity.ok(ApiResponse.success("Successfully updated all passwords", null));
    }

    @GetMapping("/disable-2fa-all")
    public ResponseEntity<?> disable2faAll() {
        authService.disableAllTwoFactor();
        return ResponseEntity.ok(ApiResponse.success("Successfully disabled 2FA for all users", null));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(@RequestBody Map<String, Object> settings) {
        String userId = SecurityUtils.getCurrentUserId();
        var user = authService.updateSettings(userId, settings);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", 
                Map.of("user", userMapper.toResponseDTO(user))));
    }
}
