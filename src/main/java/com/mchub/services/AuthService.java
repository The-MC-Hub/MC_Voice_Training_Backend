package com.mchub.services;

import com.mchub.dto.RegisterRequest;
import com.mchub.models.User;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

public interface AuthService {

    User register(@NonNull RegisterRequest req);

    LoginResponse login(@NonNull String email, @NonNull String password);

    @PreAuthorize("isAuthenticated()")
    void updatePasswordAsync(@NonNull String userId, @NonNull String plainPassword);

    void forgotPassword(@NonNull String email);

    void resetPassword(@NonNull String email, @NonNull String code, @NonNull String newPassword);

    void sendOtp(@NonNull String email);

    void verifyOtp(@NonNull String email, @NonNull String code);

    void resendOtp(@NonNull String email);

    LoginResponse verifyAdminLoginOtp(@NonNull String email, @NonNull String code);

    @PreAuthorize("isAuthenticated()")
    User updateSettings(@NonNull String userId, @NonNull Map<String, Object> settings);

    @org.springframework.scheduling.annotation.Async
    void initializeMCProfile(@NonNull String userId);

    @PreAuthorize("hasAuthority('ADMIN')")
    void fixAllSeededPasswords();

    @PreAuthorize("hasAuthority('ADMIN')")
    void disableAllTwoFactor();

    record LoginResponse(User user, String token) {
    }
}
