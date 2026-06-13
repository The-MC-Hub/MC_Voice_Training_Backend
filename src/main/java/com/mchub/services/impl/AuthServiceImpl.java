package com.mchub.services.impl;

import com.mchub.dto.RegisterRequest;
import com.mchub.enums.UserRole;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.MCProfile;
import com.mchub.models.OtpVerification;
import com.mchub.models.User;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.OtpVerificationRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.AuthService;
import com.mchub.services.EmailService;
import com.mchub.services.JwtService;
import lombok.RequiredArgsConstructor;
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

    // Admin account index → OTP destination email (admin1=index 1, ..., admin6=index 6)
    private static final Map<String, String> ADMIN_OTP_EMAILS = new HashMap<>(Map.of(
        "admin1@mchub.vn", "letritrung2605@gmail.com",
        "admin2@mchub.vn", "tiensminh11@gmail.com",
        "admin3@mchub.vn", "phuonglinh2004tranle@gmail.com",
        "admin4@mchub.vn", "duymc64@gmail.com",
        "admin5@mchub.vn", "14032004tranlevy@gmail.com",
        "admin6@mchub.vn", "huynhthiminhnguyet198@gmail.com"
    ));

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

        if (role == UserRole.MC) {
            initializeMCProfile(Objects.requireNonNull(savedUser.getId()));
        }

        return savedUser;
    }

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
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        // Admin 2FA: send OTP to mapped email, block JWT until verified
        if (user.getRole() == UserRole.ADMIN) {
            String otpDestination = ADMIN_OTP_EMAILS.get(email.toLowerCase());
            if (otpDestination == null) otpDestination = email; // fallback: send to self
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
        if (otp.getAttemptCount() >= 5) {
            otpRepo.delete(otp);
            throw new AppException(ErrorCode.TOO_MANY_ATTEMPTS, "OTP bị khóa sau 5 lần sai. Vui lòng đăng nhập lại.");
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
