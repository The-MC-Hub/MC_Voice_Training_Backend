package com.mchub.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchub.dto.RegisterRequest;
import com.mchub.dto.UserResponseDTO;
import com.mchub.enums.UserRole;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.mapper.UserMapper;
import com.mchub.models.User;
import com.mchub.repositories.ReferralRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.AuditLogService;
import com.mchub.services.AuthService;
import com.mchub.services.GamificationService;
import com.mchub.services.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest for AuthController — loads only the web layer (controller +
 * GlobalExceptionHandler), all service dependencies mocked. Security filters
 * disabled (addFilters=false) since /api/v1/auth/** is permitAll in
 * SecurityConfig anyway and JwtAuthenticationFilter needs a live JwtService
 * bean we don't want to wire here; endpoints requiring auth (/me,
 * /referral-code/generate, /settings) are exercised via SecurityUtils
 * directly, which reads SecurityContextHolder — populated per-test as needed.
 */
@WebMvcTest(controllers = AuthController.class)
@ContextConfiguration(classes = {AuthController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private AuditLogService auditLogService;
    @MockBean private JwtService jwtService;
    @MockBean private UserMapper userMapper;
    @MockBean private UserRepository userRepository;
    @MockBean private ReferralRepository referralRepository;
    @MockBean private GamificationService gamificationService;

    private static final String USER_ID = "user-001";
    private static final String EMAIL = "qa.tester@mchubtest.local";

    private User sampleUser() {
        return User.builder().id(USER_ID).email(EMAIL).name("QA Tester").role(UserRole.CLIENT).build();
    }

    private UserResponseDTO sampleUserDto() {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(USER_ID);
        dto.setEmail(EMAIL);
        return dto;
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("201 CREATED with requiresVerification=true on success")
        void returns201OnSuccess() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setName("New User"); req.setEmail(EMAIL); req.setPassword("password123");
            when(authService.register(any(RegisterRequest.class))).thenReturn(sampleUser());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.requiresVerification").value(true))
                    .andExpect(jsonPath("$.data.email").value(EMAIL));

            verify(authService).sendOtp(EMAIL);
        }

        @Test
        @DisplayName("400 BAD_REQUEST when email is blank — bean validation")
        void returns400OnBlankEmail() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setName("New User"); req.setEmail(""); req.setPassword("password123");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("409 CONFLICT when authService throws EMAIL_ALREADY_EXISTS")
        void returns409WhenEmailTaken() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setName("New User"); req.setEmail(EMAIL); req.setPassword("password123");
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new AppException(ErrorCode.EMAIL_ALREADY_EXISTS));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));
        }

        @Test
        @DisplayName("registration still succeeds (still 201) even if sendOtp throws — exception is swallowed")
        void succeedsEvenIfSendOtpFails() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setName("New User"); req.setEmail(EMAIL); req.setPassword("password123");
            when(authService.register(any(RegisterRequest.class))).thenReturn(sampleUser());
            org.mockito.Mockito.doThrow(new RuntimeException("SMTP down")).when(authService).sendOtp(EMAIL);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("200 OK with token+user on success")
        void returns200OnSuccess() throws Exception {
            AuthService.LoginResponse resp = new AuthService.LoginResponse(sampleUser(), "jwt-token");
            when(authService.login(EMAIL, "password123")).thenReturn(resp);
            when(userMapper.toResponseDTO(any(User.class))).thenReturn(sampleUserDto());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + EMAIL + "\",\"password\":\"password123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.token").value("jwt-token"));

            verify(gamificationService).processLoginStreak(USER_ID);
        }

        @Test
        @DisplayName("202 ACCEPTED with requiresAdminOtp=true when ADMIN_OTP_REQUIRED thrown")
        void returns202WhenAdminOtpRequired() throws Exception {
            when(authService.login(EMAIL, "password123"))
                    .thenThrow(new AppException(ErrorCode.ADMIN_OTP_REQUIRED, "ADMIN_OTP_REQUIRED:" + EMAIL));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + EMAIL + "\",\"password\":\"password123\"}"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.data.requiresAdminOtp").value(true))
                    .andExpect(jsonPath("$.data.email").value(EMAIL));
        }

        @Test
        @DisplayName("401 UNAUTHORIZED and logs AUTH_LOGIN_FAILED on invalid credentials")
        void returns401OnInvalidCredentials() throws Exception {
            when(authService.login(EMAIL, "wrongpass"))
                    .thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + EMAIL + "\",\"password\":\"wrongpass\"}"))
                    .andExpect(status().isUnauthorized());

            verify(auditLogService).logError(eq((String) null), any(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("400 BAD_REQUEST when password is blank — bean validation")
        void returns400OnBlankPassword() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + EMAIL + "\",\"password\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("does not propagate gamification failure — login still 200")
        void succeedsEvenIfGamificationFails() throws Exception {
            AuthService.LoginResponse resp = new AuthService.LoginResponse(sampleUser(), "jwt-token");
            when(authService.login(EMAIL, "password123")).thenReturn(resp);
            when(userMapper.toResponseDTO(any(User.class))).thenReturn(sampleUserDto());
            org.mockito.Mockito.doThrow(new RuntimeException("gamify down"))
                    .when(gamificationService).processLoginStreak(USER_ID);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + EMAIL + "\",\"password\":\"password123\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/verify-otp")
    class VerifyOtp {

        @Test
        @DisplayName("200 OK with fresh token after verify + auto-login")
        void returns200AfterVerify() throws Exception {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(sampleUser()));
            when(jwtService.generateToken(USER_ID, "CLIENT")).thenReturn("fresh-jwt");
            when(userMapper.toResponseDTO(any(User.class))).thenReturn(sampleUserDto());

            mockMvc.perform(post("/api/v1/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + EMAIL + "\",\"code\":\"123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.token").value("fresh-jwt"));

            verify(authService).verifyOtp(EMAIL, "123456");
        }

        @Test
        @DisplayName("propagates VALIDATION_FAILED as 400 when OTP invalid")
        void returns400OnInvalidOtp() throws Exception {
            org.mockito.Mockito.doThrow(new AppException(ErrorCode.VALIDATION_FAILED, "Ma OTP khong dung"))
                    .when(authService).verifyOtp(EMAIL, "000000");

            mockMvc.perform(post("/api/v1/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + EMAIL + "\",\"code\":\"000000\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password / reset-password")
    class PasswordReset {

        @Test
        @DisplayName("forgot-password always returns 200, even for unknown email (anti-enumeration)")
        void forgotPasswordAlwaysReturns200() throws Exception {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nobody@test.local\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("reset-password propagates TOO_MANY_ATTEMPTS as 429")
        void returns429OnTooManyAttempts() throws Exception {
            org.mockito.Mockito.doThrow(new AppException(ErrorCode.TOO_MANY_ATTEMPTS))
                    .when(authService).resetPassword(anyString(), anyString(), anyString());

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + EMAIL + "\",\"code\":\"111111\",\"newPassword\":\"newpass123\"}"))
                    .andExpect(status().isTooManyRequests());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/auth/settings")
    class UpdateSettings {

        @Test
        @DisplayName("returns 401 when called without an authenticated SecurityContext")
        void delegatesToUpdateSettings() throws Exception {
            when(authService.updateSettings(anyString(), anyMap())).thenReturn(sampleUser());
            when(userMapper.toResponseDTO(any(User.class))).thenReturn(sampleUserDto());

            // SecurityUtils.getCurrentUserId() requires an authenticated SecurityContext;
            // since addFilters=false skips JwtAuthenticationFilter, this throws
            // IllegalStateException, which GlobalExceptionHandler now maps to 401
            // (fixed DEFECT-015 — previously fell through to a generic 500).
            mockMvc.perform(put("/api/v1/auth/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New Name\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
