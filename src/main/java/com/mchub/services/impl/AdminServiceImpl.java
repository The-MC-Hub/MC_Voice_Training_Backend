package com.mchub.services.impl;

import com.mchub.dto.UserResponseDTO;
import com.mchub.enums.AuditAction;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.TransactionStatus;
import com.mchub.enums.UserRole;
import com.mchub.models.AuditLog;
import com.mchub.models.PaymentTransaction;
import com.mchub.models.PracticeSession;
import com.mchub.models.User;
import com.mchub.repositories.AuditLogRepository;
import com.mchub.repositories.PaymentTransactionRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.mapper.UserMapper;
import com.mchub.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

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

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PaymentTransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PracticeSessionRepository practiceSessionRepository;

    @Override
    public Map<String, Object> getAdminDashboardOverview() {
        List<PaymentTransaction> allTx = transactionRepository.findAll();
        long completedCount = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count();
        long pendingCount   = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING).count();
        long failedCount    = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.FAILED).count();
        long totalRevenue   = allTx.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                                   .mapToLong(PaymentTransaction::getAmount).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers",       userRepository.count());
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

        // ── New users per day (last 30 days) ──────────────────────────────────
        List<User> newUsers30 = userRepository.findByCreatedAtAfter(day30Ago);
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

        // ── Monthly new users (last 12 months) ────────────────────────────────
        LocalDateTime month12Ago = now.minusMonths(12);
        List<User> newUsers12M = userRepository.findByCreatedAtAfter(month12Ago);
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

        // ── Active vs inactive users ───────────────────────────────────────────
        List<User> allUsers = userRepository.findAll();
        long activeUsers   = allUsers.stream().filter(User::isActive).count();
        long inactiveUsers = allUsers.size() - activeUsers;

        // ── Users active last 7 days (had a login) ────────────────────────────
        List<AuditLog> logins7d = auditLogRepository.findByActionAndCreatedAtAfter(AuditAction.AUTH_LOGIN, day7Ago);
        long activeUsersLast7d = logins7d.stream().map(AuditLog::getUserId).distinct().count();

        // ── Plan distribution ────────────────────────────────────────────────
        Map<String, Long> planDist = new LinkedHashMap<>();
        for (SubscriptionPlan p : SubscriptionPlan.values()) {
            planDist.put(p.name(), userRepository.countByPlan(p));
        }

        // ── Role distribution ────────────────────────────────────────────────
        Map<String, Long> roleDist = new LinkedHashMap<>();
        for (UserRole r : UserRole.values()) {
            roleDist.put(r.name(), userRepository.countByRole(r));
        }

        // ── Registrations today ───────────────────────────────────────────────
        long registrationsToday = userRepository.findByCreatedAtAfter(today).size();
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
        result.put("premiumUsers",        userRepository.countByIsPremiumTrue());
        return result;
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

}
