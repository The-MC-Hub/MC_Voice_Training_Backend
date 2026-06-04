package com.mchub.services.impl;

import com.mchub.dto.UserResponseDTO;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.TransactionStatus;
import com.mchub.enums.UserRole;
import com.mchub.models.PaymentTransaction;
import com.mchub.models.User;
import com.mchub.repositories.PaymentTransactionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.mapper.UserMapper;
import com.mchub.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PaymentTransactionRepository transactionRepository;

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
    public UserResponseDTO updateUserStatus(@NonNull String id, boolean isActive, boolean isVerified) {
        User user = userRepository.findById(Objects.requireNonNull(id))
            .orElseThrow(() -> new RuntimeException("User does not exist"));
        user.setActive(isActive);
        user.setVerified(isVerified);
        return userMapper.toResponseDTO(userRepository.save(user));
    }

}
