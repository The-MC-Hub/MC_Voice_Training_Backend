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
import com.mchub.services.GoogleTokenVerifierService;
import com.mchub.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
    private final GoogleTokenVerifierService googleTokenVerifierService;

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
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email này đã được sử dụng. Vui lòng dùng email khác hoặc đăng nhập.");
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
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS, "Email hoac mat khau khong dung."));

        if (!user.isActive()) {
            throw new AppException(ErrorCode.USER_LOCKED, "Tai khoan da bi khoa. Vui long lien he ho tro.");
        }
        if (!user.isVerified()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "EMAIL_NOT_VERIFIED:" + email);
        }

        // Check temporary lockout from brute force
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.USER_LOCKED,
                    "Tài khoản tạm khóa do đăng nhập sai quá nhiều lần. Thử lại sau " + LOCKOUT_MINUTES + " phút.");
        }

        boolean isMatch = passwordEncoder.matches(password, user.getPassword());

        if (!isMatch) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            }
            userRepository.save(user);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Email hoac mat khau khong dung.");
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
    public GoogleAuthResult loginWithGoogle(@NonNull String googleIdToken) {
        var identity = googleTokenVerifierService.verify(googleIdToken);

        User existing = userRepository.findByGoogleId(identity.googleId())
                .or(() -> userRepository.findByEmail(identity.email()))
                .orElse(null);

        if (existing != null) {
            if (!existing.isActive()) {
                throw new AppException(ErrorCode.USER_LOCKED, "Tai khoan da bi khoa. Vui long lien he ho tro.");
            }
            // Auto-link: account exists (password-based or already Google-linked) — trust it,
            // since Google already verified this email.
            boolean changed = false;
            if (existing.getGoogleId() == null) {
                existing.setGoogleId(identity.googleId());
                changed = true;
            }
            if (!existing.isVerified()) {
                existing.setVerified(true);
                changed = true;
            }
            if (changed) userRepository.save(existing);

            if (existing.getRole() == UserRole.ADMIN) {
                // Keep admin 2FA guarantee intact — Google login does not bypass it.
                String otpDestination = buildAdminOtpEmails().get(existing.getEmail().toLowerCase());
                if (otpDestination == null || otpDestination.isBlank()) otpDestination = existing.getEmail();
                sendAdminLoginOtp(existing.getEmail(), otpDestination);
                throw new AppException(ErrorCode.ADMIN_OTP_REQUIRED, "ADMIN_OTP_REQUIRED:" + existing.getEmail());
            }

            String token = jwtService.generateToken(existing.getId(), existing.getRole().name());
            return new GoogleAuthResult.LoggedIn(new LoginResponse(existing, token));
        }

        String pendingToken = jwtService.generatePendingGoogleToken(identity.googleId(), identity.email(), identity.name());
        return new GoogleAuthResult.PendingRegistration(pendingToken, identity.email(), identity.name());
    }

    @Override
    public User linkGoogleAccount(@NonNull String userId, @NonNull String googleIdToken) {
        var identity = googleTokenVerifierService.verify(googleIdToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));

        if (!identity.email().equalsIgnoreCase(user.getEmail())) {
            throw new AppException(ErrorCode.GOOGLE_EMAIL_MISMATCH,
                    "Email tài khoản Google (" + identity.email() + ") không khớp với email tài khoản hiện tại.");
        }

        userRepository.findByGoogleId(identity.googleId()).ifPresent(other -> {
            if (!other.getId().equals(user.getId())) {
                throw new AppException(ErrorCode.GOOGLE_ACCOUNT_ALREADY_LINKED,
                        "Tài khoản Google này đã được liên kết với một tài khoản khác.");
            }
        });

        user.setGoogleId(identity.googleId());
        return userRepository.save(user);
    }

    @Override
    public User unlinkGoogleAccount(@NonNull String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));

        // A user whose password was never explicitly set (Google-only registration, random
        // unusable password) would be permanently locked out if we removed their only sign-in
        // method. passwordChangedAt is set by updateSettings/resetPassword — never by
        // completeGoogleRegistration — so its absence means "no real password exists".
        if (user.getPasswordChangedAt() == null) {
            throw new AppException(ErrorCode.GOOGLE_UNLINK_BLOCKED_NO_PASSWORD,
                    "Vui lòng đặt mật khẩu trước khi hủy liên kết Google, nếu không bạn sẽ không thể đăng nhập.");
        }

        user.setGoogleId(null);
        return userRepository.save(user);
    }

    @Override
    public LoginResponse completeGoogleRegistration(@NonNull String pendingToken, @NonNull String role, String referralCode) {
        var pending = jwtService.extractPendingGoogleIdentity(pendingToken);

        // Re-check in case the account was created by another request in the meantime
        // (e.g. user double-clicked "continue", or registered via password in another tab).
        if (userRepository.findByGoogleId(pending.googleId()).isPresent()
                || userRepository.findByEmail(pending.email()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email này đã được sử dụng. Vui lòng đăng nhập.");
        }

        UserRole parsedRole = UserRole.CLIENT;
        try {
            UserRole candidate = UserRole.valueOf(role.toUpperCase());
            if (candidate == UserRole.MC) parsedRole = UserRole.MC;
        } catch (IllegalArgumentException ignored) {
        }

        // Random unguessable password — Google-linked accounts never log in via password unless
        // the user later sets one explicitly through account settings.
        byte[] randomBytes = new byte[32];
        RNG.nextBytes(randomBytes);
        String unusablePassword = passwordEncoder.encode(java.util.Base64.getEncoder().encodeToString(randomBytes));

        User user = User.builder()
                .name(pending.name() != null && !pending.name().isBlank() ? pending.name() : "MC Hub User")
                .email(pending.email())
                .password(unusablePassword)
                .googleId(pending.googleId())
                .role(parsedRole)
                .isVerified(true) // Google already verified this email
                .isActive(true)
                .build();

        User savedUser = userRepository.save(Objects.requireNonNull(user));

        String generatedCode = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            String candidate = generateReferralCode();
            if (userRepository.findByReferralCode(candidate).isEmpty()) {
                generatedCode = candidate;
                break;
            }
        }
        if (generatedCode != null) savedUser.setReferralCode(generatedCode);

        if (referralCode != null && !referralCode.isBlank()) {
            userRepository.findByReferralCode(referralCode.toUpperCase().trim()).ifPresent(referrer -> {
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

        if (parsedRole == UserRole.MC) {
            initializeMCProfile(Objects.requireNonNull(savedUser.getId()));
        }

        String token = jwtService.generateToken(savedUser.getId(), savedUser.getRole().name());
        return new LoginResponse(savedUser, token);
    }

    @Override
    @Async
    public void updatePasswordAsync(@NonNull String userId, @NonNull String plainPassword) {
        userRepository.findById(Objects.requireNonNull(userId)).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(plainPassword));
            user.setPasswordChangedAt(LocalDateTime.now(ZoneOffset.UTC));
            userRepository.save(Objects.requireNonNull(user));
        });
    }



    private static final String PWD_RESET_PREFIX = "pwd_reset:";

    @Override
    public void forgotPassword(@NonNull String email) {
        // Silently succeed if email not found — prevent user enumeration
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return;

        String key = PWD_RESET_PREFIX + email;
        otpRepo.deleteAllByEmail(key);

        String code = generateOtp();
        otpRepo.save(OtpVerification.builder()
                .email(key)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build());

        String userName = user.getName() != null ? user.getName() : "ban";
        String body = """
<p style="margin:0 0 20px 0;color:#374151;font-size:14px;line-height:1.85;font-family:'Helvetica Neue',Arial,sans-serif;">
  Ban vua yeu cau dat lai mat khau tai khoan <strong style="color:#111113;">MC Hub</strong>.<br/>
  Su dung ma OTP sau de dat lai mat khau cua ban. Ma co hieu luc trong <strong>10 phut</strong>.
</p>
""" + """
<!-- OTP display -->
<table width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin:24px 0;">
  <tr>
    <td align="center">
      <div style="display:inline-block;background:#f5f5f5;border:2px dashed #f5a623;border-radius:12px;padding:16px 40px;">
        <span style="font-size:32px;font-weight:800;letter-spacing:0.4em;color:#111113;font-family:monospace;">%s</span>
      </div>
    </td>
  </tr>
</table>
<p style="color:#6b7280;font-size:12px;text-align:center;margin:0;font-family:'Helvetica Neue',Arial,sans-serif;">
  Neu ban khong yeu cau dat lai mat khau, vui long bo qua email nay.
</p>
""".formatted(formatOtpDigits(code));

        try {
            String html = emailService.buildVerificationEmail(userName, body);
            emailService.sendHtmlEmail(email, "MC Hub — Ma dat lai mat khau", html);
        } catch (Exception e) {
            emailService.sendSimpleEmail(email,
                    "MC Hub — Ma dat lai mat khau",
                    "Ma OTP dat lai mat khau cua ban: " + code + "\n\nMa co hieu luc trong 10 phut.\nNeu ban khong yeu cau, hay bo qua email nay.");
        }
    }

    @Override
    public void resetPassword(@NonNull String email, @NonNull String code, @NonNull String newPassword) {
        String key = PWD_RESET_PREFIX + email;
        OtpVerification otp = otpRepo.findTopByEmailOrderByCreatedAtDesc(key)
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_FAILED, "Khong tim thay ma OTP. Vui long yeu cau lai."));

        if (otp.isUsed())
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ma OTP da duoc su dung.");
        if (otp.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ma OTP da het han. Vui long yeu cau lai.");
        if (otp.getAttemptCount() >= 5) {
            otpRepo.delete(otp);
            throw new AppException(ErrorCode.TOO_MANY_ATTEMPTS, "OTP bi khoa sau 5 lan sai. Vui long yeu cau OTP moi.");
        }
        if (!otp.getCode().equals(code.trim())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepo.save(otp);
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ma OTP khong dung.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (newPassword.length() < 8)
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Mat khau phai co it nhat 8 ky tu.");

        // Mark used only after all validation passes
        otp.setUsed(true);
        otpRepo.save(otp);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now(ZoneOffset.UTC));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        otpRepo.deleteAllByEmail(key);
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
            user.setPasswordChangedAt(LocalDateTime.now(ZoneOffset.UTC));
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

    private String formatOtpDigits(String code) {
        StringBuilder sb = new StringBuilder();
        for (char c : code.toCharArray()) {
            sb.append("<td style=\"width:38px;height:48px;text-align:center;vertical-align:middle;")
              .append("background:#1a1a1e;border:1px solid #2a2a2e;border-radius:10px;")
              .append("font-size:22px;font-weight:800;color:#f5a623;")
              .append("font-family:'Courier New',Courier,monospace;padding:0 4px;\">")
              .append(c)
              .append("</td>");
        }
        return sb.toString();
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

        // Generate magic-link token, persist on user
        String magicToken = UUID.randomUUID().toString();
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setEmailVerificationToken(magicToken);
            userRepository.save(u);
        });

        // Send HTML email with both magic link button + OTP fallback
        String userName = userRepository.findByEmail(email)
                .map(u -> u.getName() != null ? u.getName() : "bạn")
                .orElse("bạn");

        String feUrl = emailService.getFeUrl();
        String magicLink = feUrl + "/verify-email?token=" + magicToken + "&email=" + email;

        String body = """
<p style="margin:0 0 20px 0;color:#374151;font-size:14px;line-height:1.85;font-family:'Helvetica Neue',Arial,sans-serif;">
  Chào mừng bạn đến với <strong style="color:#111113;">MC Hub</strong> — nền tảng luyện giọng AI dành cho MC chuyên nghiệp.<br/>
  Để hoàn tất đăng ký, vui lòng xác thực địa chỉ email của bạn.
</p>

<!-- Magic link button -->
<table width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin:24px 0 8px 0;">
  <tr>
    <td align="center">
      <a href="%s"
         style="display:inline-block;background:#f5a623;color:#000000;font-size:15px;font-weight:700;
                padding:16px 48px;border-radius:12px;text-decoration:none;letter-spacing:0.3px;font-family:'Helvetica Neue',Arial,sans-serif;">
        Xác nhận email ngay &#8594;
      </a>
    </td>
  </tr>
</table>
<p style="color:#6b7280;font-size:11px;text-align:center;margin:8px 0 0 0;font-family:'Helvetica Neue',Arial,sans-serif;">Nút có hiệu lực trong <strong style="color:#374151;">10 phút</strong></p>

<!-- Divider -->
<table width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin:28px 0;">
  <tr>
    <td style="border-top:1px solid #d1d5db;"></td>
    <td style="padding:0 14px;white-space:nowrap;color:#6b7280;font-size:10px;letter-spacing:1.2px;text-transform:uppercase;font-weight:600;font-family:'Helvetica Neue',Arial,sans-serif;">HOẶC DÙNG MÃ</td>
    <td style="border-top:1px solid #d1d5db;"></td>
  </tr>
</table>

<!-- OTP block -->
<table width="100%%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td align="center">
      <table cellpadding="0" cellspacing="0" border="0" style="background:#111113;border:1px solid #27272a;border-radius:16px;padding:0;">
        <tr>
          <td style="padding:22px 40px 18px;text-align:center;">
            <p style="color:#9ca3af;font-size:10px;letter-spacing:2px;text-transform:uppercase;margin:0 0 16px 0;font-weight:600;font-family:'Helvetica Neue',Arial,sans-serif;">MÃ XÁC THỰC</p>
            <table cellpadding="0" cellspacing="0" border="0" style="margin:0 auto;">
              <tr>%s</tr>
            </table>
            <p style="color:#4b5563;font-size:10px;margin:16px 0 0 0;letter-spacing:0.3px;font-family:'Helvetica Neue',Arial,sans-serif;">Không chia sẻ mã này cho bất kỳ ai</p>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
""".formatted(magicLink, formatOtpDigits(code));

        try {
            String html = emailService.buildVerificationEmail(userName, body);
            emailService.sendHtmlEmail(email, "MC Hub — Xác thực tài khoản của bạn", html);
        } catch (Exception e) {
            // Fallback plain text if HTML fails
            emailService.sendSimpleEmail(email,
                    "MCHub — Mã xác thực tài khoản",
                    "Mã OTP của bạn là: " + code + "\n\nXác thực nhanh: " + magicLink + "\n\nMã có hiệu lực trong 10 phút.");
        }
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
            user.setEmailVerificationToken(null); // clear magic-link token on OTP verify
            userRepository.save(user);
        });
        otpRepo.deleteAllByEmail(email);
    }

    @Override
    public LoginResponse verifyEmailByToken(@NonNull String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_FAILED, "Link xác thực không hợp lệ hoặc đã hết hạn."));
        if (user.isVerified()) {
            // Already verified — just return login response
            String jwt = jwtService.generateToken(user.getId(), user.getRole().name());
            return new LoginResponse(user, jwt);
        }
        user.setVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);
        otpRepo.deleteAllByEmail(user.getEmail());
        String jwt = jwtService.generateToken(user.getId(), user.getRole().name());
        return new LoginResponse(user, jwt);
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
