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

    LoginResponse verifyEmailByToken(@NonNull String token);

    void resendOtp(@NonNull String email);

    LoginResponse verifyAdminLoginOtp(@NonNull String email, @NonNull String code);

    /**
     * Handles a verified Google ID token: if the email already has an account (password-based
     * or previously Google-linked), links/logs in immediately. If not, returns a pending
     * registration result carrying a short-lived token the client must send back to
     * completeGoogleRegistration along with the chosen role.
     */
    GoogleAuthResult loginWithGoogle(@NonNull String googleIdToken);

    LoginResponse completeGoogleRegistration(@NonNull String pendingToken, @NonNull String role, String referralCode);

    sealed interface GoogleAuthResult permits GoogleAuthResult.LoggedIn, GoogleAuthResult.PendingRegistration {
        record LoggedIn(LoginResponse response) implements GoogleAuthResult {
        }

        record PendingRegistration(String pendingToken, String email, String name) implements GoogleAuthResult {
        }
    }

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
