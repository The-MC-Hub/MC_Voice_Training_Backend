package com.mchub.services.impl;

import com.mchub.dto.RegisterRequest;
import com.mchub.enums.UserRole;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.OtpVerification;
import com.mchub.models.User;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.OtpVerificationRepository;
import com.mchub.repositories.ReferralRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.AuthService;
import com.mchub.services.EmailService;
import com.mchub.services.GoogleTokenVerifierService;
import com.mchub.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthServiceImpl. Mocks all repositories/dependencies —
 * no real MongoDB/SMTP required. Covers the highest-risk business rules:
 * brute-force lockout, admin 2FA gate, OTP validation/attempt-limits,
 * password reset, and duplicate-registration handling.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private MCProfileRepository mcProfileRepository;
    @Mock private EmailService emailService;
    @Mock private OtpVerificationRepository otpRepo;
    @Mock private ReferralRepository referralRepository;
    @Mock private GoogleTokenVerifierService googleTokenVerifierService;

    private AuthServiceImpl authService;

    private static final String EMAIL = "qa.tester@mchubtest.local";
    private static final String USER_ID = "user-auth-001";

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, passwordEncoder, jwtService, mcProfileRepository,
                emailService, otpRepo, referralRepository, googleTokenVerifierService);
        ReflectionTestUtils.setField(authService, "adminOtpEmail1", "");
        ReflectionTestUtils.setField(authService, "adminOtpEmail2", "");
        ReflectionTestUtils.setField(authService, "adminOtpEmail3", "");
        ReflectionTestUtils.setField(authService, "adminOtpEmail4", "");
        ReflectionTestUtils.setField(authService, "adminOtpEmail5", "");
        ReflectionTestUtils.setField(authService, "adminOtpEmail6", "");
    }

    private User.UserBuilder verifiedActiveUser() {
        return User.builder()
                .id(USER_ID).email(EMAIL).name("QA Tester")
                .password("$2a$10$hashedBcryptValue")
                .role(UserRole.CLIENT).isActive(true).isVerified(true)
                .failedLoginAttempts(0);
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("throws INVALID_CREDENTIALS when email not found")
        void throwsWhenEmailNotFound() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(EMAIL, "password123"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("throws USER_LOCKED when account isActive=false")
        void throwsWhenInactive() {
            User user = verifiedActiveUser().isActive(false).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(EMAIL, "password123"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_LOCKED);
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED with EMAIL_NOT_VERIFIED prefix when unverified")
        void throwsWhenUnverified() {
            User user = verifiedActiveUser().isVerified(false).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(EMAIL, "password123"))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("EMAIL_NOT_VERIFIED:" + EMAIL);
        }

        @Test
        @DisplayName("throws USER_LOCKED when lockedUntil is in the future (brute-force lockout active)")
        void throwsWhenTemporarilyLocked() {
            User user = verifiedActiveUser().lockedUntil(LocalDateTime.now().plusMinutes(5)).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(EMAIL, "password123"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_LOCKED);
        }

        @Test
        @DisplayName("increments failedLoginAttempts and locks account after reaching MAX_FAILED_ATTEMPTS (10)")
        void locksAccountAfterMaxFailedAttempts() {
            User user = verifiedActiveUser().failedLoginAttempts(9).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(EMAIL, "wrongpassword"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            assertThat(user.getFailedLoginAttempts()).isEqualTo(10);
            assertThat(user.getLockedUntil()).isNotNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("does NOT lock account before reaching MAX_FAILED_ATTEMPTS")
        void doesNotLockBeforeMax() {
            User user = verifiedActiveUser().failedLoginAttempts(2).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(EMAIL, "wrongpassword"))
                    .isInstanceOf(AppException.class);

            assertThat(user.getFailedLoginAttempts()).isEqualTo(3);
            assertThat(user.getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("resets failedLoginAttempts/lockedUntil on successful login")
        void resetsLockoutOnSuccess() {
            User user = verifiedActiveUser().failedLoginAttempts(4)
                    .lockedUntil(LocalDateTime.now().minusMinutes(1)) // already expired
                    .build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateToken(USER_ID, "CLIENT")).thenReturn("jwt-token");

            AuthService.LoginResponse response = authService.login(EMAIL, "password123");

            assertThat(response.token()).isEqualTo("jwt-token");
            assertThat(user.getFailedLoginAttempts()).isZero();
            assertThat(user.getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("plain-text legacy password is rejected — no plaintext fallback")
        void plainTextPasswordIsRejected() {
            User user = verifiedActiveUser().password("plaintext123").build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(EMAIL, "plaintext123"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(jwtService, never()).generateToken(anyString(), anyString());
        }

        @Test
        @DisplayName("ADMIN role: blocks JWT issuance and throws ADMIN_OTP_REQUIRED, sends OTP email first")
        void adminLoginRequiresOtp() {
            User admin = verifiedActiveUser().role(UserRole.ADMIN).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(EMAIL, "password123"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ADMIN_OTP_REQUIRED);

            verify(emailService).sendSimpleEmail(eq(EMAIL), anyString(), anyString());
            verify(jwtService, never()).generateToken(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("verifyAdminLoginOtp")
    class VerifyAdminLoginOtp {

        private final String otpKey = "admin_login:" + EMAIL;

        @Test
        @DisplayName("issues JWT on correct OTP and marks it used")
        void issuesJwtOnCorrectOtp() {
            OtpVerification otp = OtpVerification.builder()
                    .email(otpKey).code("123456").used(false).attemptCount(0)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(otpKey)).thenReturn(Optional.of(otp));
            User admin = verifiedActiveUser().role(UserRole.ADMIN).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));
            when(jwtService.generateToken(USER_ID, "ADMIN")).thenReturn("admin-jwt");

            AuthService.LoginResponse response = authService.verifyAdminLoginOtp(EMAIL, "123456");

            assertThat(response.token()).isEqualTo("admin-jwt");
            assertThat(otp.isUsed()).isTrue();
            verify(otpRepo).deleteAllByEmail(otpKey);
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED and increments attemptCount on wrong code")
        void wrongCodeIncrementsAttempts() {
            OtpVerification otp = OtpVerification.builder()
                    .email(otpKey).code("123456").used(false).attemptCount(1)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(otpKey)).thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authService.verifyAdminLoginOtp(EMAIL, "000000"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            assertThat(otp.getAttemptCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("throws TOO_MANY_ATTEMPTS and deletes OTP after 3 failed attempts")
        void deletesOtpAfterThreeFailedAttempts() {
            OtpVerification otp = OtpVerification.builder()
                    .email(otpKey).code("123456").used(false).attemptCount(3)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(otpKey)).thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authService.verifyAdminLoginOtp(EMAIL, "000000"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TOO_MANY_ATTEMPTS);

            verify(otpRepo).delete(otp);
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED when OTP already used")
        void throwsWhenAlreadyUsed() {
            OtpVerification otp = OtpVerification.builder()
                    .email(otpKey).code("123456").used(true).attemptCount(0)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(otpKey)).thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authService.verifyAdminLoginOtp(EMAIL, "123456"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED when OTP expired")
        void throwsWhenExpired() {
            OtpVerification otp = OtpVerification.builder()
                    .email(otpKey).code("123456").used(false).attemptCount(0)
                    .expiresAt(LocalDateTime.now().minusMinutes(1)).build();
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(otpKey)).thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authService.verifyAdminLoginOtp(EMAIL, "123456"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        private final String otpKey = "pwd_reset:" + EMAIL;

        @Test
        @DisplayName("updates password, resets lockout counters, marks OTP used, deletes all reset OTPs")
        void resetsPasswordSuccessfully() {
            OtpVerification otp = OtpVerification.builder()
                    .email(otpKey).code("654321").used(false).attemptCount(0)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(otpKey)).thenReturn(Optional.of(otp));
            User user = verifiedActiveUser().failedLoginAttempts(5)
                    .lockedUntil(LocalDateTime.now().plusMinutes(10)).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newSecurePass123")).thenReturn("hashed-new-pass");

            authService.resetPassword(EMAIL, "654321", "newSecurePass123");

            assertThat(user.getPassword()).isEqualTo("hashed-new-pass");
            assertThat(user.getFailedLoginAttempts()).isZero();
            assertThat(user.getLockedUntil()).isNull();
            assertThat(otp.isUsed()).isTrue();
            verify(otpRepo).deleteAllByEmail(otpKey);
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED when new password is shorter than 8 chars")
        void throwsWhenPasswordTooShort() {
            OtpVerification otp = OtpVerification.builder()
                    .email(otpKey).code("654321").used(false).attemptCount(0)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(otpKey)).thenReturn(Optional.of(otp));
            User user = verifiedActiveUser().build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resetPassword(EMAIL, "654321", "short"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            // OTP must NOT be consumed since password validation failed after code check
            assertThat(otp.isUsed()).isFalse();
        }

        @Test
        @DisplayName("throws TOO_MANY_ATTEMPTS and deletes OTP after 5 failed attempts")
        void deletesOtpAfterFiveFailedAttempts() {
            OtpVerification otp = OtpVerification.builder()
                    .email(otpKey).code("654321").used(false).attemptCount(5)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(otpKey)).thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authService.resetPassword(EMAIL, "000000", "newSecurePass123"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TOO_MANY_ATTEMPTS);

            verify(otpRepo).delete(otp);
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND when email does not match any user (after OTP validated)")
        void throwsWhenUserMissing() {
            OtpVerification otp = OtpVerification.builder()
                    .email(otpKey).code("654321").used(false).attemptCount(0)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(otpKey)).thenReturn(Optional.of(otp));
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(EMAIL, "654321", "newSecurePass123"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("silently returns (no exception, no email) when email not found — prevents user enumeration")
        void silentlyReturnsWhenEmailNotFound() {
            when(userRepository.findByEmail("nobody@test.local")).thenReturn(Optional.empty());

            authService.forgotPassword("nobody@test.local");

            verify(emailService, never()).sendSimpleEmail(anyString(), anyString(), anyString());
            verify(otpRepo, never()).save(any(OtpVerification.class));
        }

        @Test
        @DisplayName("generates and stores a fresh OTP when email exists")
        void generatesOtpWhenEmailExists() throws Exception {
            User user = verifiedActiveUser().build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(emailService.buildVerificationEmail(anyString(), anyString())).thenReturn("<html></html>");

            authService.forgotPassword(EMAIL);

            verify(otpRepo).deleteAllByEmail("pwd_reset:" + EMAIL);
            ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
            verify(otpRepo).save(captor.capture());
            assertThat(captor.getValue().getCode()).matches("\\d{6}");
            assertThat(captor.getValue().getEmail()).isEqualTo("pwd_reset:" + EMAIL);
        }

        @Test
        @DisplayName("falls back to plain-text email when HTML send fails")
        void fallsBackToPlainTextOnHtmlFailure() throws Exception {
            User user = verifiedActiveUser().build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(emailService.buildVerificationEmail(anyString(), anyString()))
                    .thenThrow(new RuntimeException("template engine down"));

            authService.forgotPassword(EMAIL);

            verify(emailService).sendSimpleEmail(eq(EMAIL), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("verifyOtp")
    class VerifyOtp {

        @Test
        @DisplayName("marks user verified and clears magic-link token on correct OTP")
        void verifiesUserOnCorrectOtp() {
            OtpVerification otp = OtpVerification.builder()
                    .email(EMAIL).code("111222").used(false).attemptCount(0)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(EMAIL)).thenReturn(Optional.of(otp));
            User user = verifiedActiveUser().isVerified(false).emailVerificationToken("some-token").build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            authService.verifyOtp(EMAIL, "111222");

            assertThat(user.isVerified()).isTrue();
            assertThat(user.getEmailVerificationToken()).isNull();
            verify(otpRepo).deleteAllByEmail(EMAIL);
        }

        @Test
        @DisplayName("throws TOO_MANY_ATTEMPTS and deletes OTP after 5 failed attempts")
        void deletesOtpAfterFiveFailedAttempts() {
            OtpVerification otp = OtpVerification.builder()
                    .email(EMAIL).code("111222").used(false).attemptCount(5)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(EMAIL)).thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authService.verifyOtp(EMAIL, "000000"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TOO_MANY_ATTEMPTS);
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED when no OTP record exists")
        void throwsWhenNoOtpFound() {
            when(otpRepo.findTopByEmailOrderByCreatedAtDesc(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyOtp(EMAIL, "111222"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("verifyEmailByToken")
    class VerifyEmailByToken {

        @Test
        @DisplayName("throws VALIDATION_FAILED for unknown/expired token")
        void throwsForUnknownToken() {
            when(userRepository.findByEmailVerificationToken("bad-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmailByToken("bad-token"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("verifies user and issues JWT for a valid unverified token")
        void verifiesAndIssuesJwt() {
            User user = verifiedActiveUser().isVerified(false).emailVerificationToken("good-token").build();
            when(userRepository.findByEmailVerificationToken("good-token")).thenReturn(Optional.of(user));
            when(jwtService.generateToken(USER_ID, "CLIENT")).thenReturn("verify-jwt");

            AuthService.LoginResponse response = authService.verifyEmailByToken("good-token");

            assertThat(response.token()).isEqualTo("verify-jwt");
            assertThat(user.isVerified()).isTrue();
            assertThat(user.getEmailVerificationToken()).isNull();
            verify(otpRepo).deleteAllByEmail(EMAIL);
        }

        @Test
        @DisplayName("issues JWT immediately without re-saving when already verified (idempotent)")
        void idempotentWhenAlreadyVerified() {
            User user = verifiedActiveUser().isVerified(true).emailVerificationToken("good-token").build();
            when(userRepository.findByEmailVerificationToken("good-token")).thenReturn(Optional.of(user));
            when(jwtService.generateToken(USER_ID, "CLIENT")).thenReturn("verify-jwt");

            authService.verifyEmailByToken("good-token");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("throws EMAIL_ALREADY_EXISTS when a verified user already owns the email")
        void throwsWhenEmailTakenByVerifiedUser() {
            RegisterRequest req = new RegisterRequest();
            req.setName("New User"); req.setEmail(EMAIL); req.setPassword("password123");

            User existing = verifiedActiveUser().isVerified(true).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("cleans up and replaces a stale UNVERIFIED account with the same email")
        void replacesStaleUnverifiedAccount() {
            RegisterRequest req = new RegisterRequest();
            req.setName("New User"); req.setEmail(EMAIL); req.setPassword("password123");

            User stale = verifiedActiveUser().isVerified(false).mcProfile(null).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(stale));
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByReferralCode(anyString())).thenReturn(Optional.empty());

            authService.register(req);

            verify(otpRepo).deleteAllByEmail(EMAIL);
            verify(userRepository).delete(stale);
        }

        @Test
        @DisplayName("defaults role to CLIENT for an unparseable/absent role string")
        void defaultsToClientRole() {
            RegisterRequest req = new RegisterRequest();
            req.setName("New User"); req.setEmail("fresh@test.local"); req.setPassword("password123");
            req.setRole("NOT_A_ROLE");

            when(userRepository.findByEmail("fresh@test.local")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByReferralCode(anyString())).thenReturn(Optional.empty());

            authService.register(req);

            assertThat(captor.getValue().getRole()).isEqualTo(UserRole.CLIENT);
        }

        @Test
        @DisplayName("new user starts unverified and active — email verification required before login")
        void newUserStartsUnverified() {
            RegisterRequest req = new RegisterRequest();
            req.setName("New User"); req.setEmail("fresh2@test.local"); req.setPassword("password123");

            when(userRepository.findByEmail("fresh2@test.local")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByReferralCode(anyString())).thenReturn(Optional.empty());

            authService.register(req);

            assertThat(captor.getValue().isVerified()).isFalse();
            assertThat(captor.getValue().isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("updateSettings")
    class UpdateSettings {

        @Test
        @DisplayName("only updates keys present in the settings map")
        void onlyUpdatesPresentKeys() {
            User user = verifiedActiveUser().name("Old Name").bio("Old bio").build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            authService.updateSettings(USER_ID, java.util.Map.of("name", "New Name"));

            assertThat(user.getName()).isEqualTo("New Name");
            assertThat(user.getBio()).isEqualTo("Old bio"); // untouched
        }

        @Test
        @DisplayName("hashes password and sets passwordChangedAt when 'password' key present")
        void hashesPasswordWhenPresent() {
            User user = verifiedActiveUser().build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(passwordEncoder.encode("newPass123")).thenReturn("hashed-new");

            authService.updateSettings(USER_ID, java.util.Map.of("password", "newPass123"));

            assertThat(user.getPassword()).isEqualTo("hashed-new");
            assertThat(user.getPasswordChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND for unknown userId")
        void throwsWhenUserMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.updateSettings(USER_ID, java.util.Map.of("name", "X")))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("resendOtp")
    class ResendOtp {

        @Test
        @DisplayName("throws USER_NOT_FOUND for unknown email, never sends OTP")
        void throwsWhenEmailUnknown() {
            when(userRepository.findByEmail("nobody@test.local")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resendOtp("nobody@test.local"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(otpRepo, never()).save(any(OtpVerification.class));
        }
    }
}
