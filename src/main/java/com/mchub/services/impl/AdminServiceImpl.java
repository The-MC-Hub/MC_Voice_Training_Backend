package com.mchub.services.impl;

import com.mchub.dto.UserResponseDTO;
import com.mchub.enums.AuditAction;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.TransactionStatus;
import com.mchub.enums.UserRole;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.AuditLog;
import com.mchub.models.OtpVerification;
import com.mchub.models.PaymentTransaction;
import com.mchub.models.PracticeSession;
import com.mchub.models.User;
import com.mchub.models.UserStats;
import com.mchub.repositories.AuditLogRepository;
import com.mchub.repositories.OtpVerificationRepository;
import com.mchub.repositories.PaymentTransactionRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.UserStatsRepository;
import com.mchub.mapper.UserMapper;
import com.mchub.services.AdminService;
import com.mchub.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PaymentTransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final OtpVerificationRepository otpRepo;
    private final UserStatsRepository userStatsRepository;
    private final com.mchub.repositories.DiscountCodeRepository discountCodeRepository;

    private static final SecureRandom ADMIN_RNG = new SecureRandom();

    @Override
    public Map<String, Object> getAdminDashboardOverview() {
        List<PaymentTransaction> allTx = transactionRepository.findAll();
        long completedCount = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count();
        long pendingCount   = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING).count();
        long failedCount    = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.FAILED).count();
        long totalRevenue   = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                                   .mapToLong(PaymentTransaction::getAmount).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers",       userRepository.countByRoleNot(UserRole.ADMIN));
        stats.put("totalMCs",         userRepository.countByRole(UserRole.MC));
        stats.put("totalTransactions", allTx.size());
        stats.put("completedTransactions", completedCount);
        stats.put("pendingTransactions",   pendingCount);
        stats.put("failedTransactions",    failedCount);
        stats.put("totalRevenue",      totalRevenue);
        return stats;
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
            .map(userMapper::toResponseDTO).toList();
    }

    @Override
    public UserResponseDTO getUserById(@NonNull String userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponseDTO)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));
    }

    @Override
    public List<UserResponseDTO> getAllMCs() {
        return userRepository.findByRole(UserRole.MC).stream()
            .map(userMapper::toResponseDTO).toList();
    }

    @Override
    public List<Map<String, Object>> getAllTransactions() {
        List<PaymentTransaction> txList = transactionRepository.findAllByOrderByCreatedAtDesc();
        // Bulk-fetch users to avoid N+1
        List<String> userIds = txList.stream().map(PaymentTransaction::getUserId).distinct().toList();
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return txList.stream().map(tx -> {
            User user = userMap.get(tx.getUserId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",          tx.getId());
            row.put("orderCode",   tx.getOrderCode());
            row.put("userId",      tx.getUserId());
            row.put("userName",    user != null ? user.getName() : "Unknown");
            row.put("userEmail",   user != null ? user.getEmail() : "");
            row.put("amount",      tx.getAmount());
            row.put("plan",        tx.getPlan() != null ? tx.getPlan().name() : "");
            row.put("status",      tx.getStatus() != null ? tx.getStatus().name() : "");
            row.put("memo",        tx.getMemo());
            row.put("bankRef",     tx.getBankRef());
            row.put("createdAt",   tx.getCreatedAt());
            row.put("completedAt", tx.getCompletedAt());
            return row;
        }).toList();
    }

    @Override
    public Map<String, Object> getRevenueStats() {
        List<PaymentTransaction> allTx = transactionRepository.findAll();

        // Revenue by status
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("COMPLETED", allTx.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED).mapToLong(PaymentTransaction::getAmount).sum());
        byStatus.put("PENDING",   allTx.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING).mapToLong(PaymentTransaction::getAmount).sum());
        byStatus.put("FAILED",    allTx.stream().filter(t -> t.getStatus() == TransactionStatus.FAILED).mapToLong(PaymentTransaction::getAmount).sum());

        // Revenue by plan (completed only)
        Map<String, Long> byPlan = new LinkedHashMap<>();
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            long sum = allTx.stream()
                    .filter(t -> t.getStatus() == TransactionStatus.COMPLETED && t.getPlan() == plan)
                    .mapToLong(PaymentTransaction::getAmount).sum();
            if (sum > 0) byPlan.put(plan.name(), sum);
        }

        // Monthly revenue (completed) — key = "YYYY-MM"
        Map<String, Long> monthly = allTx.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED && t.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        t -> String.format("%d-%02d", t.getCreatedAt().getYear(), t.getCreatedAt().getMonthValue()),
                        Collectors.summingLong(PaymentTransaction::getAmount)
                ));

        // Count by status
        Map<String, Long> countByStatus = new LinkedHashMap<>();
        countByStatus.put("COMPLETED", allTx.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count());
        countByStatus.put("PENDING",   allTx.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING).count());
        countByStatus.put("FAILED",    allTx.stream().filter(t -> t.getStatus() == TransactionStatus.FAILED).count());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("revenueByStatus",  byStatus);
        result.put("revenueByPlan",    byPlan);
        result.put("monthlyRevenue",   monthly);
        result.put("countByStatus",    countByStatus);
        result.put("totalRevenue",     byStatus.get("COMPLETED"));
        return result;
    }

    @Override
    public Map<String, Object> getAnalytics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime day30Ago = now.minusDays(30);
        LocalDateTime day7Ago  = now.minusDays(7);
        LocalDateTime today    = now.toLocalDate().atStartOfDay();
        Instant inst30Ago = day30Ago.toInstant(ZoneOffset.UTC);
        Instant instToday = today.toInstant(ZoneOffset.UTC);

        // ── New users per day (last 30 days) — exclude ADMIN ─────────────────
        List<User> newUsers30 = userRepository.findByCreatedAtAfterAndRoleNot(day30Ago, UserRole.ADMIN);
        Map<String, Long> newUsersByDay = new TreeMap<>();
        for (int i = 29; i >= 0; i--) {
            String key = now.minusDays(i).toLocalDate().toString();
            newUsersByDay.put(key, 0L);
        }
        newUsers30.forEach(u -> {
            if (u.getCreatedAt() != null) {
                String key = u.getCreatedAt().toLocalDate().toString();
                newUsersByDay.merge(key, 1L, Long::sum);
            }
        });

        // ── Logins per day (last 30 days) from AuditLog ─────────────────────
        List<AuditLog> logins30 = auditLogRepository.findByActionAndCreatedAtAfter(AuditAction.AUTH_LOGIN, day30Ago);
        Map<String, Long> loginsByDay = new TreeMap<>();
        for (int i = 29; i >= 0; i--) {
            loginsByDay.put(now.minusDays(i).toLocalDate().toString(), 0L);
        }
        logins30.forEach(a -> {
            if (a.getCreatedAt() != null) {
                String key = a.getCreatedAt().toLocalDate().toString();
                loginsByDay.merge(key, 1L, Long::sum);
            }
        });

        // ── Logins per hour today (0–23) ──────────────────────────────────────
        List<AuditLog> loginsToday = auditLogRepository.findByActionAndCreatedAtAfter(AuditAction.AUTH_LOGIN, today);
        Map<String, Long> loginsByHour = new TreeMap<>();
        for (int h = 0; h < 24; h++) loginsByHour.put(String.format("%02d:00", h), 0L);
        loginsToday.forEach(a -> {
            if (a.getCreatedAt() != null) {
                String key = String.format("%02d:00", a.getCreatedAt().getHour());
                loginsByHour.merge(key, 1L, Long::sum);
            }
        });

        // ── Practice sessions per day (last 30 days) ──────────────────────────
        List<PracticeSession> sessions30 = practiceSessionRepository.findByCreatedAtAfter(inst30Ago);
        Map<String, Long> sessionsByDay = new TreeMap<>();
        for (int i = 29; i >= 0; i--) {
            sessionsByDay.put(now.minusDays(i).toLocalDate().toString(), 0L);
        }
        sessions30.forEach(s -> {
            if (s.getCreatedAt() != null) {
                String key = s.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
                sessionsByDay.merge(key, 1L, Long::sum);
            }
        });

        // ── Practice sessions per hour today ─────────────────────────────────
        List<PracticeSession> sessionsToday = practiceSessionRepository.findByCreatedAtAfter(instToday);
        Map<String, Long> sessionsByHour = new TreeMap<>();
        for (int h = 0; h < 24; h++) sessionsByHour.put(String.format("%02d:00", h), 0L);
        sessionsToday.forEach(s -> {
            if (s.getCreatedAt() != null) {
                String key = String.format("%02d:00", s.getCreatedAt().atZone(ZoneOffset.UTC).getHour());
                sessionsByHour.merge(key, 1L, Long::sum);
            }
        });

        // ── Monthly new users (last 12 months) — exclude ADMIN ───────────────
        LocalDateTime month12Ago = now.minusMonths(12);
        List<User> newUsers12M = userRepository.findByCreatedAtAfterAndRoleNot(month12Ago, UserRole.ADMIN);
        Map<String, Long> newUsersByMonth = new TreeMap<>();
        for (int i = 11; i >= 0; i--) {
            LocalDateTime m = now.minusMonths(i);
            newUsersByMonth.put(String.format("%d-%02d", m.getYear(), m.getMonthValue()), 0L);
        }
        newUsers12M.forEach(u -> {
            if (u.getCreatedAt() != null) {
                String key = String.format("%d-%02d", u.getCreatedAt().getYear(), u.getCreatedAt().getMonthValue());
                newUsersByMonth.merge(key, 1L, Long::sum);
            }
        });

        // ── Monthly logins (last 12 months) ───────────────────────────────────
        List<AuditLog> logins12M = auditLogRepository.findByActionAndCreatedAtAfter(AuditAction.AUTH_LOGIN, month12Ago);
        Map<String, Long> loginsByMonth = new TreeMap<>();
        for (int i = 11; i >= 0; i--) {
            LocalDateTime m = now.minusMonths(i);
            loginsByMonth.put(String.format("%d-%02d", m.getYear(), m.getMonthValue()), 0L);
        }
        logins12M.forEach(a -> {
            if (a.getCreatedAt() != null) {
                String key = String.format("%d-%02d", a.getCreatedAt().getYear(), a.getCreatedAt().getMonthValue());
                loginsByMonth.merge(key, 1L, Long::sum);
            }
        });

        // ── Active vs inactive users — exclude ADMIN ─────────────────────────
        List<User> allUsers = userRepository.findByRoleNot(UserRole.ADMIN);
        long activeUsers   = allUsers.stream().filter(User::isActive).count();
        long inactiveUsers = allUsers.size() - activeUsers;

        // ── Users active last 7 days (had a login) ────────────────────────────
        List<AuditLog> logins7d = auditLogRepository.findByActionAndCreatedAtAfter(AuditAction.AUTH_LOGIN, day7Ago);
        long activeUsersLast7d = logins7d.stream().map(AuditLog::getUserId).distinct().count();

        // ── Plan distribution — exclude ADMIN ────────────────────────────────
        Map<String, Long> planDist = new LinkedHashMap<>();
        for (SubscriptionPlan p : SubscriptionPlan.values()) {
            planDist.put(p.name(), userRepository.countByPlanAndRoleNot(p, UserRole.ADMIN));
        }

        // ── Role distribution — exclude ADMIN ────────────────────────────────
        Map<String, Long> roleDist = new LinkedHashMap<>();
        for (UserRole r : UserRole.values()) {
            if (r == UserRole.ADMIN) continue;
            roleDist.put(r.name(), userRepository.countByRole(r));
        }

        // ── Registrations today — exclude ADMIN ──────────────────────────────
        long registrationsToday = userRepository.findByCreatedAtAfterAndRoleNot(today, UserRole.ADMIN).size();
        long loginsToday30      = loginsToday.size();
        long sessionsToday30    = sessionsToday.size();

        // ── Top activity hours (peak hour from last 30 days logins) ──────────
        Map<String, Long> loginsByHour30 = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) loginsByHour30.put(String.format("%02d:00", h), 0L);
        logins30.forEach(a -> {
            if (a.getCreatedAt() != null) {
                String key = String.format("%02d:00", a.getCreatedAt().getHour());
                loginsByHour30.merge(key, 1L, Long::sum);
            }
        });

        Map<String, Object> result = new LinkedHashMap<>();
        // Daily
        result.put("newUsersByDay",       toChartList(newUsersByDay,    "date", "count"));
        result.put("loginsByDay",         toChartList(loginsByDay,      "date", "count"));
        result.put("sessionsByDay",       toChartList(sessionsByDay,    "date", "count"));
        // Hourly
        result.put("loginsByHour",        toChartList(loginsByHour,     "hour", "count"));
        result.put("sessionsByHour",      toChartList(sessionsByHour,   "hour", "count"));
        result.put("loginsByHour30d",     toChartList(loginsByHour30,   "hour", "count"));
        // Monthly
        result.put("newUsersByMonth",     toChartList(newUsersByMonth,  "month", "count"));
        result.put("loginsByMonth",       toChartList(loginsByMonth,    "month", "count"));
        // Distributions
        result.put("planDistribution",    planDist);
        result.put("roleDistribution",    roleDist);
        // Totals
        result.put("totalUsers",          allUsers.size());
        result.put("activeUsers",         activeUsers);
        result.put("inactiveUsers",       inactiveUsers);
        result.put("activeUsersLast7d",   activeUsersLast7d);
        result.put("registrationsToday",  registrationsToday);
        result.put("loginsToday",         loginsToday30);
        result.put("sessionsToday",       sessionsToday30);
        result.put("totalLogins30d",      (long) logins30.size());
        result.put("totalSessions30d",    (long) sessions30.size());
        result.put("premiumUsers",        userRepository.countByIsPremiumTrueAndRoleNot(UserRole.ADMIN));
        return result;
    }

    // ── Admin user management ─────────────────────────────────────────────────

    @Override
    public UserResponseDTO createUser(@NonNull String name, @NonNull String email,
                                      @NonNull String password, @NonNull String role,
                                      String phoneNumber, String adminNote, String plan, String couponId) {
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email already in use");
        }
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            userRole = UserRole.CLIENT;
        }

        User.UserBuilder builder = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(userRole)
                .isVerified(true)
                .isActive(true);

        if (phoneNumber != null && !phoneNumber.isBlank()) builder.phoneNumber(phoneNumber);
        if (adminNote   != null && !adminNote.isBlank())   builder.bio(adminNote);

        if (plan != null && !plan.isBlank() && !plan.equalsIgnoreCase("FREE")) {
            try {
                com.mchub.enums.SubscriptionPlan sp = com.mchub.enums.SubscriptionPlan.valueOf(plan.toUpperCase());
                builder.isPremium(true)
                       .plan(sp)
                       .planExpiresAt(LocalDateTime.now().plusDays(com.mchub.config.PlanConfig.daysFor(sp)))
                       .aiSessionsUsed(0);
            } catch (IllegalArgumentException ignored) {}
        }

        User saved = userRepository.save(Objects.requireNonNull(builder.build()));

        if (couponId != null && !couponId.isBlank()) {
            discountCodeRepository.findById(couponId).ifPresent(dc -> {
                dc.setUsedCount(dc.getUsedCount() + 1);
                discountCodeRepository.save(dc);
            });
        }

        log.info(">>> ADMIN CREATE USER: email={} role={} plan={} coupon={}", email, role, plan, couponId);
        return userMapper.toResponseDTO(saved);
    }

    @Override
    public void sendPasswordResetEmail(@NonNull String userId) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));
        String code = String.format("%06d", ADMIN_RNG.nextInt(1_000_000));
        otpRepo.deleteAllByEmail(user.getEmail());
        otpRepo.save(OtpVerification.builder()
                .email(user.getEmail())
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build());
        emailService.sendSimpleEmail(user.getEmail(),
                "MCHub — Đặt lại mật khẩu",
                "Quản trị viên đã gửi mã đặt lại mật khẩu cho bạn.\n\nMã OTP: " + code
                + "\n\nMã có hiệu lực trong 30 phút.\nTruy cập trang web và nhập mã này để đặt lại mật khẩu.");
    }

    @Override
    public void changeUserPassword(@NonNull String userId, @NonNull String newPassword) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void deleteUser(@NonNull String userId) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));
        // Soft delete: deactivate instead of hard delete to preserve data integrity
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public Map<String, Object> getUserStats(@NonNull String userId) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));

        List<PracticeSession> sessions = practiceSessionRepository
                .findByUserIdOrderByCreatedAtDesc(userId);

        long totalSessions = sessions.size();
        double avgScore = sessions.stream()
                .filter(s -> s.getOverallScore() > 0)
                .mapToDouble(PracticeSession::getOverallScore)
                .average().orElse(0.0);
        double bestScore = sessions.stream()
                .mapToDouble(PracticeSession::getOverallScore)
                .max().orElse(0.0);

        List<Map<String, Object>> recentSessions = sessions.stream().limit(5).map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("lessonId", s.getLessonId());
            m.put("overallScore", s.getOverallScore());
            m.put("accuracyScore", s.getAccuracyScore());
            m.put("rhythmScore", s.getRhythmScore());
            m.put("speakingRateWpm", s.getSpeakingRateWpm());
            m.put("createdAt", s.getCreatedAt());
            return m;
        }).toList();

        UserStats stats = userStatsRepository.findByUserId(userId).orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("userName", user.getName());
        result.put("email", user.getEmail());
        result.put("totalSessions", totalSessions);
        result.put("avgScore", Math.round(avgScore * 10.0) / 10.0);
        result.put("bestScore", Math.round(bestScore * 10.0) / 10.0);
        result.put("recentSessions", recentSessions);
        result.put("aiSessionsUsed", user.getAiSessionsUsed());
        result.put("plan", user.getPlan() != null ? user.getPlan().name() : "FREE");
        result.put("createdAt", user.getCreatedAt());

        if (stats != null) {
            result.put("currentStreak", stats.getCurrentStreak());
            result.put("longestStreak", stats.getLongestStreak());
            result.put("totalPracticeHours", stats.getTotalPracticeHours());
            result.put("cumulativeXP", stats.getCumulativeXP());
            result.put("currentTier", stats.getCurrentTier());
            result.put("weeklyXP", stats.getWeeklyXP());
            result.put("lastPracticeTime", stats.getLastPracticeTime());
        } else {
            result.put("currentStreak", 0);
            result.put("longestStreak", 0);
            result.put("totalPracticeHours", 0.0);
            result.put("cumulativeXP", 0.0);
            result.put("currentTier", "BRONZE");
            result.put("weeklyXP", 0.0);
            result.put("lastPracticeTime", null);
        }

        return result;
    }

    @Override
    public void sendNotificationEmail(@NonNull String userId, @NonNull String subject, @NonNull String content) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));
        emailService.sendSimpleEmail(user.getEmail(), subject, content);
    }

    private List<Map<String, Object>> toChartList(Map<String, Long> data, String keyField, String valueField) {
        List<Map<String, Object>> list = new ArrayList<>();
        data.forEach((k, v) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put(keyField, k);
            item.put(valueField, v);
            list.add(item);
        });
        return list;
    }

    @Override
    public UserResponseDTO updateUserStatus(@NonNull String id, boolean isActive, boolean isVerified) {
        User user = userRepository.findById(Objects.requireNonNull(id))
            .orElseThrow(() -> new RuntimeException("User does not exist"));
        user.setActive(isActive);
        user.setVerified(isVerified);
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    @Override
    public UserResponseDTO updateUserPlan(@NonNull String id, @NonNull String planStr) {
        User user = userRepository.findById(Objects.requireNonNull(id))
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
        
        if (planStr.equalsIgnoreCase("FREE")) {
            user.setPremium(false);
            user.setPlan(SubscriptionPlan.FREE);
            user.setPlanExpiresAt(null);
        } else {
            try {
                SubscriptionPlan sp = SubscriptionPlan.valueOf(planStr.toUpperCase());
                user.setPremium(true);
                user.setPlan(sp);
                user.setPlanExpiresAt(LocalDateTime.now().plusDays(com.mchub.config.PlanConfig.daysFor(sp)));
                user.setAiSessionsUsed(0);
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Invalid plan: " + planStr);
            }
        }
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    @Override
    public Map<String, Object> getGrowthAnalytics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime day30Ago = now.minusDays(30);
        LocalDateTime day7Ago  = now.minusDays(7);
        LocalDateTime day1Ago  = now.minusDays(1);

        List<User> allUsers    = userRepository.findByRoleNot(UserRole.ADMIN);
        List<PaymentTransaction> allTx = transactionRepository.findAll();

        // ── DAU / MAU ────────────────────────────────────────────────────────
        // MAU = distinct users who logged in last 30 days
        List<AuditLog> logins30 = auditLogRepository.findByActionAndCreatedAtAfter(AuditAction.AUTH_LOGIN, day30Ago);
        long mau = logins30.stream().map(AuditLog::getUserId).distinct().count();
        // DAU = distinct users who logged in last 24h
        List<AuditLog> logins1d = auditLogRepository.findByActionAndCreatedAtAfter(AuditAction.AUTH_LOGIN, day1Ago);
        long dau = logins1d.stream().map(AuditLog::getUserId).distinct().count();
        double dauMauRatio = mau > 0 ? Math.round((double) dau / mau * 1000.0) / 10.0 : 0.0;

        // ── Conversion funnel ────────────────────────────────────────────────
        long totalUsers    = allUsers.size();
        long premiumUsers  = allUsers.stream().filter(User::isPremium).count();
        long basicUsers    = allUsers.stream().filter(u -> u.getPlan() == SubscriptionPlan.BASIC).count();
        long fullUsers     = allUsers.stream().filter(u -> u.getPlan() == SubscriptionPlan.FULL).count();
        long annualUsers   = allUsers.stream().filter(u -> u.getPlan() == SubscriptionPlan.ANNUAL).count();
        long freeUsers     = totalUsers - premiumUsers;
        double conversionRate = totalUsers > 0 ? Math.round((double) premiumUsers / totalUsers * 1000.0) / 10.0 : 0.0;

        // ── ARPU / ARPPU / LTV ───────────────────────────────────────────────
        long totalRevenue  = allTx.stream()
            .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
            .mapToLong(PaymentTransaction::getAmount).sum();
        double arpu  = totalUsers  > 0 ? Math.round((double) totalRevenue / totalUsers)  : 0;
        double arppu = premiumUsers > 0 ? Math.round((double) totalRevenue / premiumUsers) : 0;
        // LTV estimate: ARPPU * avg subscription months (approx 4 months avg for mixed plans)
        double avgSubMonths = 4.0;
        double ltv = Math.round(arppu * avgSubMonths);

        // ── MRR estimate ──────────────────────────────────────────────────────
        // BASIC=199k/mo, FULL=299k/mo, ANNUAL=1990k/12=165.8k/mo
        long mrr = (basicUsers * 199000L) + (fullUsers * 299000L) + (long)(annualUsers * 165833L);

        // ── Feature adoption: users who practiced within 7d of registration ──
        List<PracticeSession> allSessions = practiceSessionRepository.findAll();
        // Map userId -> first session time
        java.util.Map<String, java.time.Instant> firstSessionByUser = allSessions.stream()
            .filter(s -> s.getUserId() != null && s.getCreatedAt() != null)
            .collect(java.util.stream.Collectors.toMap(
                PracticeSession::getUserId,
                PracticeSession::getCreatedAt,
                (a, b) -> a.isBefore(b) ? a : b
            ));
        long usersWhoAdoptedFeature = allUsers.stream().filter(u -> {
            java.time.Instant firstSession = firstSessionByUser.get(u.getId());
            if (firstSession == null || u.getCreatedAt() == null) return false;
            java.time.Instant regTime = u.getCreatedAt().toInstant(java.time.ZoneOffset.UTC);
            return java.time.Duration.between(regTime, firstSession).toDays() <= 7;
        }).count();
        double featureAdoptionRate = totalUsers > 0 ? Math.round((double) usersWhoAdoptedFeature / totalUsers * 1000.0) / 10.0 : 0.0;

        // ── User segments (Warm/Hot/Cold based on engagement) ─────────────────
        // Hot: logged in last 7 days AND has session
        // Warm: logged in last 30 days OR has any session
        // Cold: never logged in last 30 days
        java.util.Set<String> loggedIn7d = auditLogRepository
            .findByActionAndCreatedAtAfter(AuditAction.AUTH_LOGIN, day7Ago)
            .stream().map(AuditLog::getUserId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> loggedIn30d = logins30.stream()
            .map(AuditLog::getUserId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> hasSession = firstSessionByUser.keySet();

        long hotUsers  = allUsers.stream().filter(u -> loggedIn7d.contains(u.getId()) && hasSession.contains(u.getId())).count();
        long warmUsers = allUsers.stream().filter(u -> !loggedIn7d.contains(u.getId()) && (loggedIn30d.contains(u.getId()) || hasSession.contains(u.getId()))).count();
        long coldUsers = allUsers.stream().filter(u -> !loggedIn30d.contains(u.getId())).count();

        // ── New users last 7d vs 7d-14d (growth rate) — exclude ADMIN ────────
        LocalDateTime day14Ago = now.minusDays(14);
        long newUsers7d    = userRepository.findByCreatedAtAfterAndRoleNot(day7Ago, UserRole.ADMIN).size();
        long newUsers7to14 = userRepository.findByCreatedAtBetweenAndRoleNot(day14Ago, day7Ago, UserRole.ADMIN).size();
        double userGrowthRate = newUsers7to14 > 0
            ? Math.round((double)(newUsers7d - newUsers7to14) / newUsers7to14 * 1000.0) / 10.0
            : (newUsers7d > 0 ? 100.0 : 0.0);

        // ── Cohort retention (last 3 months) ─────────────────────────────────
        List<Map<String, Object>> cohortRetention = new ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            LocalDateTime cohortStart = now.minusMonths(i + 1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime cohortEnd   = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            List<User> cohortUsers = userRepository.findByCreatedAtBetweenAndRoleNot(cohortStart, cohortEnd, UserRole.ADMIN);
            int cohortSize = cohortUsers.size();
            java.util.Set<String> cohortIds = cohortUsers.stream().map(User::getId).collect(java.util.stream.Collectors.toSet());

            // retained = still active (logged in since cohort end)
            long retained = logins30.stream()
                .filter(a -> cohortIds.contains(a.getUserId()))
                .map(AuditLog::getUserId).distinct().count();

            double retentionRate = cohortSize > 0 ? Math.round((double) retained / cohortSize * 1000.0) / 10.0 : 0.0;
            String monthLabel = String.format("%d-%02d", cohortStart.getYear(), cohortStart.getMonthValue());

            Map<String, Object> cohort = new LinkedHashMap<>();
            cohort.put("month", monthLabel);
            cohort.put("cohortSize", cohortSize);
            cohort.put("retained", retained);
            cohort.put("retentionRate", retentionRate);
            cohortRetention.add(cohort);
        }

        // ── Assemble result ───────────────────────────────────────────────────
        Map<String, Object> result = new LinkedHashMap<>();

        // Engagement
        result.put("dau", dau);
        result.put("mau", mau);
        result.put("dauMauRatio", dauMauRatio);

        // Funnel
        result.put("totalUsers",    totalUsers);
        result.put("freeUsers",     freeUsers);
        result.put("premiumUsers",  premiumUsers);
        result.put("basicUsers",    basicUsers);
        result.put("fullUsers",     fullUsers);
        result.put("annualUsers",   annualUsers);
        result.put("conversionRate", conversionRate);

        // Revenue metrics
        result.put("totalRevenue", totalRevenue);
        result.put("arpu",  (long) arpu);
        result.put("arppu", (long) arppu);
        result.put("ltv",   (long) ltv);
        result.put("mrr",   mrr);

        // Growth
        result.put("newUsers7d",      newUsers7d);
        result.put("userGrowthRate",  userGrowthRate);

        // Feature adoption
        result.put("featureAdoptionRate", featureAdoptionRate);

        // Segments
        result.put("hotUsers",  hotUsers);
        result.put("warmUsers", warmUsers);
        result.put("coldUsers", coldUsers);

        // Cohort
        result.put("cohortRetention", cohortRetention);

        return result;
    }

}
