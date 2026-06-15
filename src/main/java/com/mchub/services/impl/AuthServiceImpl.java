package com.mchub.services.impl;

import com.mchub.dto.RegisterRequest;
import com.mchub.enums.UserRole;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.MCProfile;
import com.mchub.models.OtpVerification;
import com.mchub.models.User;
import com.mchub.models.Referral;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.OtpVerificationRepository;
import com.mchub.repositories.ReferralRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.AuthService;
import com.mchub.services.EmailService;
import com.mchub.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MCProfileRepository mcProfileRepository;
    private final EmailService emailService;
    private final OtpVerificationRepository otpRepo;
    private final ReferralRepository referralRepository;

    @Value("${mchub.admin.otp.email.1:}") private String adminOtpEmail1;
    @Value("${mchub.admin.otp.email.2:}") private String adminOtpEmail2;
    @Value("${mchub.admin.otp.email.3:}") private String adminOtpEmail3;
    @Value("${mchub.admin.otp.email.4:}") private String adminOtpEmail4;
    @Value("${mchub.admin.otp.email.5:}") private String adminOtpEmail5;
    @Value("${mchub.admin.otp.email.6:}") private String adminOtpEmail6;

    private Map<String, String> buildAdminOtpEmails() {
        Map<String, String> m = new HashMap<>();
        if (!adminOtpEmail1.isBlank()) m.put("admin1@mchub.vn", adminOtpEmail1);
        if (!adminOtpEmail2.isBlank()) m.put("admin2@mchub.vn", adminOtpEmail2);
        if (!adminOtpEmail3.isBlank()) m.put("admin3@mchub.vn", adminOtpEmail3);
        if (!adminOtpEmail4.isBlank()) m.put("admin4@mchub.vn", adminOtpEmail4);
        if (!adminOtpEmail5.isBlank()) m.put("admin5@mchub.vn", adminOtpEmail5);
        if (!adminOtpEmail6.isBlank()) m.put("admin6@mchub.vn", adminOtpEmail6);
        return m;
    }

    @Override
    public User register(@NonNull RegisterRequest req) {
        userRepository.findByEmail(req.getEmail()).ifPresent(existing -> {
            if (existing.isVerified()) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email is already in use");
            }
            // Unverified stale account — clean up so user can re-register
            otpRepo.deleteAllByEmail(req.getEmail());
            if (existing.getMcProfile() != null) {
                mcProfileRepository.deleteByUser(existing.getId());
            }
            userRepository.delete(existing);
        });

        UserRole role = UserRole.CLIENT;
        if (req.getRole() != null) {
            try {
                UserRole parsed = UserRole.valueOf(req.getRole().toUpperCase());
                if (parsed == UserRole.MC)
                    role = UserRole.MC;
            } catch (IllegalArgumentException ignored) {
            }
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .phoneNumber(req.getPhoneNumber())
                .bio(req.getBio() != null ? req.getBio() : "")
                .isVerified(false)

                .isActive(true)
                .build();

        User savedUser = userRepository.save(Objects.requireNonNull(user));

        // Generate referral code (retry on collision)
        String code = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            String candidate = generateReferralCode();
            if (userRepository.findByReferralCode(candidate).isEmpty()) {
                code = candidate;
                break;
            }
        }
        if (code != null) {
            savedUser.setReferralCode(code);
        }

        // Process incoming referral code
        if (req.getReferralCode() != null && !req.getReferralCode().isBlank()) {
            userRepository.findByReferralCode(req.getReferralCode().toUpperCase().trim()).ifPresent(referrer -> {
                referralRepository.save(Referral.builder()
                        .referrerId(referrer.getId())
                        .referredUserId(savedUser.getId())
                        .build());
                referrer.setReferralCount(referrer.getReferralCount() + 1);
                userRepository.save(referrer);
                savedUser.setReferralCount(savedUser.getReferralCount() + 1);
            });
        }

        userRepository.save(savedUser);

        if (role == UserRole.MC) {
            initializeMCProfile(Objects.requireNonNull(savedUser.getId()));
        }

        return savedUser;
    }

    private static final String REFERRAL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom REFERRAL_RANDOM = new SecureRandom();

    private String generateReferralCode() {
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(REFERRAL_CHARS.charAt(REFERRAL_RANDOM.nextInt(REFERRAL_CHARS.length())));
        }
        return sb.toString();
    }

    private static final int MAX_FAILED_ATTEMPTS = 10;
    private static final int LOCKOUT_MINUTES = 15;

    @Override
    public LoginResponse login(@NonNull String email, @NonNull String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

        if (!user.isActive()) {
            throw new AppException(ErrorCode.USER_LOCKED, "Account is locked. Please contact support.");
        }
        if (!user.isVerified()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "EMAIL_NOT_VERIFIED:" + email);
        }

        // Check temporary lockout from brute force
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.USER_LOCKED,
                    "Tài khoản tạm khóa do đăng nhập sai quá nhiều lần. Thử lại sau " + LOCKOUT_MINUTES + " phút.");
        }

        boolean isMatch;
        if (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$")) {
            isMatch = passwordEncoder.matches(password, user.getPassword());
        } else {
            isMatch = password.equals(user.getPassword());
            if (isMatch && user.getId() != null) {
                updatePasswordAsync(Objects.requireNonNull(user.getId()), password);
            }
        }

        if (!isMatch) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            }
            userRepository.save(user);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        // Success — reset lockout counters
        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        // Admin 2FA: send OTP to mapped email, block JWT until verified
        if (user.getRole() == UserRole.ADMIN) {
            String otpDestination = buildAdminOtpEmails().get(email.toLowerCase());
            if (otpDestination == null || otpDestination.isBlank()) otpDestination = email;
            sendAdminLoginOtp(email, otpDestination);
            throw new AppException(ErrorCode.ADMIN_OTP_REQUIRED, "ADMIN_OTP_REQUIRED:" + email);
        }

        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return new LoginResponse(user, token);
    }

    private void sendAdminLoginOtp(@NonNull String adminEmail, @NonNull String destination) {
        otpRepo.deleteAllByEmail("admin_login:" + adminEmail);
        String code = generateOtp();
        otpRepo.save(OtpVerification.builder()
                .email("admin_login:" + adminEmail)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build());
        emailService.sendSimpleEmail(destination,
                "MCHub Admin — Mã xác thực đăng nhập",
                "Mã OTP đăng nhập admin của bạn là: " + code + "\n\nMã có hiệu lực trong 10 phút.\nNếu bạn không thực hiện đăng nhập này, hãy bỏ qua email này.");
    }

    @Override
    public LoginResponse verifyAdminLoginOtp(@NonNull String email, @NonNull String code) {
        OtpVerification otp = otpRepo.findTopByEmailOrderByCreatedAtDesc("admin_login:" + email)
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_FAILED, "Không tìm thấy mã OTP"));
        if (otp.isUsed()) throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã OTP đã được sử dụng");
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã OTP đã hết hạn");
        if (otp.getAttemptCount() >= 3) {
            otpRepo.delete(otp);
            throw new AppException(ErrorCode.TOO_MANY_ATTEMPTS, "OTP bị khóa sau 3 lần sai. Vui lòng đăng nhập lại.");
        }
        if (!otp.getCode().equals(code.trim())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepo.save(otp);
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã OTP không đúng");
        }
        otp.setUsed(true);
        otpRepo.save(otp);
        otpRepo.deleteAllByEmail("admin_login:" + email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Admin not found"));
        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return new LoginResponse(user, token);
    }



    @Override
    @Async
    public void updatePasswordAsync(@NonNull String userId, @NonNull String plainPassword) {
        userRepository.findById(Objects.requireNonNull(userId)).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(plainPassword));
            user.setPasswordChangedAt(LocalDateTime.now());
            userRepository.save(Objects.requireNonNull(user));
        });
    }



    @Override
    public void forgotPassword(@NonNull String email) {
        throw new UnsupportedOperationException("Not implemented yet after 2FA removal");
    }

    @Override
    public void resetPassword(@NonNull String email, @NonNull String code, @NonNull String newPassword) {
        throw new UnsupportedOperationException("Not implemented yet after 2FA removal");
    }

    @Override
    public User updateSettings(@NonNull String userId, @NonNull Map<String, Object> settings) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (settings.containsKey("name"))
            user.setName((String) settings.get("name"));
        if (settings.containsKey("phoneNumber"))
            user.setPhoneNumber((String) settings.get("phoneNumber"));
        if (settings.containsKey("avatar"))
            user.setAvatar((String) settings.get("avatar"));
        if (settings.containsKey("bio"))
            user.setBio((String) settings.get("bio"));
        if (settings.containsKey("password")) {
            user.setPassword(passwordEncoder.encode((String) settings.get("password")));
            user.setPasswordChangedAt(LocalDateTime.now());
        }

        return userRepository.save(user);
    }

    @Override
    public void fixAllSeededPasswords() {
        List<User> users = userRepository.findAll();
        String hashedPassword = passwordEncoder.encode("password123");
        for (User u : users) {
            u.setPassword(hashedPassword);
        }
        userRepository.saveAll(users);
    }

    @Override
    public void disableAllTwoFactor() {
        // All 2FA fields removed from User model. 
        // This method is now a no-op as the feature is fully decommissioned.
    }

    // ── OTP ───────────────────────────────────────────────────────────────

    private static final SecureRandom RNG = new SecureRandom();

    private String generateOtp() {
        return String.format("%06d", RNG.nextInt(1_000_000));
    }

    @Override
    public void sendOtp(@NonNull String email) {
        otpRepo.deleteAllByEmail(email);
        String code = generateOtp();
        otpRepo.save(OtpVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build());
        emailService.sendSimpleEmail(email,
                "MCHub — Mã xác thực tài khoản",
                "Mã OTP của bạn là: " + code + "\n\nMã có hiệu lực trong 10 phút. Không chia sẻ mã này cho ai.");
    }

    @Override
    public void verifyOtp(@NonNull String email, @NonNull String code) {
        OtpVerification otp = otpRepo.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_FAILED, "Không tìm thấy mã OTP"));
        if (otp.isUsed()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã OTP đã được sử dụng");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã OTP đã hết hạn");
        }
        if (otp.getAttemptCount() >= 5) {
            otpRepo.delete(otp);
            throw new AppException(ErrorCode.TOO_MANY_ATTEMPTS, "OTP bị khóa sau 5 lần sai. Vui lòng yêu cầu OTP mới.");
        }
        if (!otp.getCode().equals(code.trim())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepo.save(otp);
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã OTP không đúng");
        }
        otp.setUsed(true);
        otpRepo.save(otp);

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setVerified(true);
            userRepository.save(user);
        });
        otpRepo.deleteAllByEmail(email);
    }

    @Override
    public void resendOtp(@NonNull String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Email không tồn tại"));
        sendOtp(email);
    }

    @Override
    @Async
    public void initializeMCProfile(@NonNull String userId) {
        MCProfile profile = MCProfile.builder()
                .user(userId)
                .biography("")
                .build();
        MCProfile saved = mcProfileRepository.save(Objects.requireNonNull(profile));
        userRepository.findById(userId).ifPresent(u -> {
            u.setMcProfile(saved.getId());
            userRepository.save(u);
        });
    }
}
