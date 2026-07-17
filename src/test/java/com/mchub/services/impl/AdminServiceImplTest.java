package com.mchub.services.impl;

import com.mchub.dto.UserResponseDTO;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.UserRole;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.mapper.UserMapper;
import com.mchub.models.User;
import com.mchub.repositories.AuditLogRepository;
import com.mchub.repositories.DiscountCodeRepository;
import com.mchub.repositories.OtpVerificationRepository;
import com.mchub.repositories.PaymentTransactionRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.UserStatsRepository;
import com.mchub.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AdminServiceImpl. Mocks every repository/dependency —
 * runs without a real MongoDB connection (embedded Mongo is unavailable
 * on this environment; see testing/testing.md section 7).
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PaymentTransactionRepository transactionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PracticeSessionRepository practiceSessionRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpVerificationRepository otpRepo;
    @Mock private UserStatsRepository userStatsRepository;
    @Mock private DiscountCodeRepository discountCodeRepository;

    private AdminServiceImpl adminService;

    private static final String USER_ID = "6a59a5136324a762855e6003";

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(
                userRepository, userMapper, transactionRepository, auditLogRepository,
                practiceSessionRepository, emailService, passwordEncoder, otpRepo,
                userStatsRepository, discountCodeRepository);
    }

    private User.UserBuilder baseUser() {
        return User.builder()
                .id(USER_ID)
                .name("QA Tester")
                .email("qa.tester@mchubtest.local")
                .role(UserRole.CLIENT)
                .isActive(true)
                .isVerified(true)
                .isPremium(false)
                .plan(SubscriptionPlan.FREE);
    }

    // ── updateUserStatus — verifies the DEFECT fixed during audit:
    //    must throw AppException(USER_NOT_FOUND), not a raw RuntimeException,
    //    so GlobalExceptionHandler maps it to HTTP 404 instead of 500.
    @Nested
    @DisplayName("updateUserStatus")
    class UpdateUserStatus {

        @Test
        @DisplayName("updates isActive/isVerified and returns mapped DTO when user exists")
        void updatesStatusWhenUserExists() {
            User user = baseUser().build();
            UserResponseDTO dto = new UserResponseDTO(); dto.setId(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toResponseDTO(any(User.class))).thenReturn(dto);

            UserResponseDTO result = adminService.updateUserStatus(USER_ID, false, true);

            assertThat(result).isEqualTo(dto);
            assertThat(user.isActive()).isFalse();
            assertThat(user.isVerified()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("throws AppException(USER_NOT_FOUND) — not a raw RuntimeException — when user missing")
        void throwsAppExceptionWhenUserMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateUserStatus(USER_ID, true, true))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateUserPlan")
    class UpdateUserPlan {

        @Test
        @DisplayName("downgrades to FREE and clears planExpiresAt (case-insensitive 'free')")
        void downgradesToFree() {
            User user = baseUser().plan(SubscriptionPlan.BASIC).isPremium(true).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toResponseDTO(any(User.class))).thenReturn(new UserResponseDTO());

            adminService.updateUserPlan(USER_ID, "free");

            assertThat(user.getPlan()).isEqualTo(SubscriptionPlan.FREE);
            assertThat(user.isPremium()).isFalse();
            assertThat(user.getPlanExpiresAt()).isNull();
        }

        @Test
        @DisplayName("upgrades to a paid plan, sets isPremium, planExpiresAt and resets aiSessionsUsed")
        void upgradesToPaidPlan() {
            User user = baseUser().aiSessionsUsed(5).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toResponseDTO(any(User.class))).thenReturn(new UserResponseDTO());

            adminService.updateUserPlan(USER_ID, "BASIC");

            assertThat(user.getPlan()).isEqualTo(SubscriptionPlan.BASIC);
            assertThat(user.isPremium()).isTrue();
            assertThat(user.getPlanExpiresAt()).isNotNull();
            assertThat(user.getAiSessionsUsed()).isZero();
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED for an unparseable plan string (unlike createUser, which silently falls back)")
        void throwsOnInvalidPlanString() {
            User user = baseUser().build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> adminService.updateUserPlan(USER_ID, "NOT_A_PLAN"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND when id does not exist")
        void throwsWhenUserMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateUserPlan(USER_ID, "BASIC"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteUser — must be a soft delete")
    class DeleteUser {

        @Test
        @DisplayName("sets isActive=false and saves — never calls a hard-delete method")
        void softDeletesUser() {
            User user = baseUser().isActive(true).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            adminService.deleteUser(USER_ID);

            assertThat(user.isActive()).isFalse();
            verify(userRepository).save(user);
            verify(userRepository, never()).delete(any());
            verify(userRepository, never()).deleteById(anyString());
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND when id does not exist")
        void throwsWhenUserMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.deleteUser(USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("throws EMAIL_ALREADY_EXISTS when email is taken")
        void throwsWhenEmailTaken() {
            when(userRepository.existsByEmail("taken@test.local")).thenReturn(true);

            assertThatThrownBy(() -> adminService.createUser(
                    "Name", "taken@test.local", "pass1234", "CLIENT", null, null, null, null))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("falls back to CLIENT role silently for an unparseable role string")
        void fallsBackToClientForInvalidRole() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toResponseDTO(any(User.class))).thenReturn(new UserResponseDTO());

            adminService.createUser("Name", "new@test.local", "pass1234", "HACKER_ROLE", null, null, null, null);

            assertThat(captor.getValue().getRole()).isEqualTo(UserRole.CLIENT);
        }

        @Test
        @DisplayName("created user is immediately verified and active — no email OTP step required")
        void createdUserIsVerifiedAndActive() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toResponseDTO(any(User.class))).thenReturn(new UserResponseDTO());

            adminService.createUser("Name", "new@test.local", "pass1234", "CLIENT", null, null, null, null);

            assertThat(captor.getValue().isVerified()).isTrue();
            assertThat(captor.getValue().isActive()).isTrue();
        }

        @Test
        @DisplayName("an invalid plan string is silently ignored (user stays on default FREE) — no exception")
        void invalidPlanStringSilentlyIgnored() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toResponseDTO(any(User.class))).thenReturn(new UserResponseDTO());

            adminService.createUser("Name", "new@test.local", "pass1234", "CLIENT", null, null, "NOT_A_PLAN", null);

            assertThat(captor.getValue().isPremium()).isFalse();
        }

        @Test
        @DisplayName("increments discount usedCount when a valid couponId is supplied")
        void incrementsDiscountUsedCount() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toResponseDTO(any(User.class))).thenReturn(new UserResponseDTO());

            com.mchub.models.DiscountCode coupon = com.mchub.models.DiscountCode.builder()
                    .id("coupon-1").usedCount(2).build();
            when(discountCodeRepository.findById("coupon-1")).thenReturn(Optional.of(coupon));

            adminService.createUser("Name", "new@test.local", "pass1234", "CLIENT", null, null, null, "coupon-1");

            assertThat(coupon.getUsedCount()).isEqualTo(3);
            verify(discountCodeRepository).save(coupon);
        }
    }

    @Nested
    @DisplayName("getUserById / getAllMCs")
    class Lookups {

        @Test
        @DisplayName("throws USER_NOT_FOUND for an unknown id")
        void throwsWhenUserMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.getUserById(USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("returns only users with role=MC")
        void returnsOnlyMcRole() {
            User mc = baseUser().role(UserRole.MC).build();
            when(userRepository.findByRole(UserRole.MC)).thenReturn(List.of(mc));
            UserResponseDTO mcDto = new UserResponseDTO();
            mcDto.setRole(UserRole.MC);
            when(userMapper.toResponseDTO(mc)).thenReturn(mcDto);

            List<UserResponseDTO> result = adminService.getAllMCs();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRole()).isEqualTo(UserRole.MC);
        }
    }

    @Nested
    @DisplayName("changeUserPassword / sendPasswordResetEmail")
    class PasswordOps {

        @Test
        @DisplayName("changeUserPassword hashes the new password and persists it")
        void changesPassword() {
            User user = baseUser().build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newSecret123")).thenReturn("hashed-secret");

            adminService.changeUserPassword(USER_ID, "newSecret123");

            assertThat(user.getPassword()).isEqualTo("hashed-secret");
            assertThat(user.getPasswordChangedAt()).isNotNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("sendPasswordResetEmail clears prior OTPs and stores a fresh 6-digit code")
        void sendsResetEmailWithFreshOtp() {
            User user = baseUser().build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            adminService.sendPasswordResetEmail(USER_ID);

            verify(otpRepo).deleteAllByEmail(user.getEmail());
            ArgumentCaptor<com.mchub.models.OtpVerification> otpCaptor =
                    ArgumentCaptor.forClass(com.mchub.models.OtpVerification.class);
            verify(otpRepo).save(otpCaptor.capture());
            assertThat(otpCaptor.getValue().getCode()).matches("\\d{6}");
            verify(emailService).sendSimpleEmail(eq(user.getEmail()), anyString(), anyString());
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND when target user does not exist")
        void throwsWhenUserMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.sendPasswordResetEmail(USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("sendNotificationEmail")
    class NotificationEmail {

        @Test
        @DisplayName("delegates to EmailService with the target user's real email")
        void sendsNotification() {
            User user = baseUser().build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            adminService.sendNotificationEmail(USER_ID, "Subject", "Body");

            verify(emailService).sendSimpleEmail(user.getEmail(), "Subject", "Body");
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND when target user does not exist")
        void throwsWhenUserMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.sendNotificationEmail(USER_ID, "S", "B"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
            verify(emailService, never()).sendSimpleEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("getUserStats")
    class UserStats {

        @Test
        @DisplayName("falls back to default gamification values (BRONZE tier, zeroed XP) when no UserStats record exists")
        void fallsBackWhenNoStatsRecord() {
            User user = baseUser().build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(practiceSessionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());
            when(userStatsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            var result = adminService.getUserStats(USER_ID);

            assertThat(result.get("currentTier")).isEqualTo("BRONZE");
            assertThat(result.get("currentStreak")).isEqualTo(0);
            assertThat(result.get("totalSessions")).isEqualTo(0L);
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND for an unknown id")
        void throwsWhenUserMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.getUserStats(USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }
}
