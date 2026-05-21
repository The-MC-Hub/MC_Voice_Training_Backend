package com.mchub.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchub.dto.ApiResponse;
import com.mchub.enums.TransactionStatus;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.PaymentTransaction;
import com.mchub.models.User;
import com.mchub.repositories.PaymentTransactionRepository;
import com.mchub.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final UserRepository userRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Value("${mchub.payos.checksum-key:}")
    private String checksumKey;

    @Value("${mchub.payment.bank-id:MBBank}")
    private String bankId;

    @Value("${mchub.payment.account-no:190356789999}")
    private String accountNo;

    @Value("${mchub.payment.account-name:THE MC HUB ACADEMY}")
    private String accountName;

    @Value("${mchub.payment.amount:20000}")
    private int paymentAmount;

    @Value("${mchub.payment.memo-prefix:MCHUBPREMIUM}")
    private String memoPrefix;

    // ================================================================
    //  POST /api/v1/payment/create-order
    //  Tạo VietQR checkout và ghi PENDING transaction vào DB
    // ================================================================
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPremiumOrder(@RequestParam String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (user.isPremium()) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PAID, "User is already Premium");
        }

        String memo   = memoPrefix + " " + user.getId();
        String encodedName = accountName.replace(" ", "%20");
        String qrUrl  = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankId, accountNo, paymentAmount, memo, encodedName);

        // Ghi PENDING transaction (idempotent: không tạo trùng nếu memo đã tồn tại)
        if (!transactionRepository.existsByMemo(memo)) {
            transactionRepository.save(PaymentTransaction.builder()
                    .userId(userId)
                    .amount(paymentAmount)
                    .status(TransactionStatus.PENDING)
                    .memo(memo)
                    .build());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("amount", paymentAmount);
        data.put("originalAmount", 100000);
        data.put("discount", 100000 - paymentAmount);
        data.put("memo", memo);
        data.put("qrUrl", qrUrl);
        data.put("bankName", "Military Commercial Joint Stock Bank (MBBank)");
        data.put("accountNumber", accountNo);
        data.put("accountName", accountName);

        return ResponseEntity.ok(ApiResponse.success("Checkout details generated", data));
    }

    // ================================================================
    //  POST /api/v1/payment/webhook
    //  Nhận webhook từ SePay/PayOS — verify HMAC-SHA256 nếu có key
    // ================================================================
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> handlePaymentWebhook(
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestBody String rawBody) {

        log.info(">>> Received payment webhook. signature={}", signature);

        // Signature verification (bỏ qua nếu checksumKey chưa cấu hình)
        if (checksumKey != null && !checksumKey.isBlank()) {
            if (signature == null || !verifyHmac(rawBody, signature)) {
                log.warn(">>> Webhook signature mismatch. Rejected.");
                throw new AppException(ErrorCode.WEBHOOK_INVALID_SIGNATURE, "Invalid webhook signature");
            }
        }

        // Parse body
        Map<String, Object> webhookData;
        try {
            webhookData = objectMapper.readValue(rawBody, Map.class);
        } catch (JsonProcessingException e) {
            log.error(">>> Webhook body parse failed: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success("Webhook ignored (parse error)", null));
        }

        // Trích xuất memo từ nhiều field khác nhau của các provider
        String memo = extractMemo(webhookData);
        String bankRef = extractString(webhookData, "transactionID", "referenceCode", "id");

        log.info(">>> Webhook memo={}, bankRef={}", memo, bankRef);

        if (memo != null && memo.toUpperCase().contains(memoPrefix.toUpperCase())) {
            String[] parts = memo.trim().split("\\s+");
            if (parts.length >= 2) {
                String userId = parts[1].trim();
                User user = userRepository.findById(userId).orElse(null);

                if (user != null && !user.isPremium()) {
                    // Cập nhật transaction PENDING → COMPLETED
                    transactionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                            .ifPresent(tx -> {
                                tx.setStatus(TransactionStatus.COMPLETED);
                                tx.setBankRef(bankRef);
                                tx.setWebhookRaw(rawBody);
                                tx.setCompletedAt(LocalDateTime.now());
                                transactionRepository.save(tx);
                            });

                    user.setPremium(true);
                    userRepository.save(user);
                    log.info(">>> USER UPGRADED TO PREMIUM: {}", user.getEmail());
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Webhook processed", null));
    }

    // ================================================================
    //  GET /api/v1/payment/status/{userId}
    //  Frontend polling — kiểm tra trạng thái premium
    // ================================================================
    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentStatus(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        List<PaymentTransaction> txList = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("isPremium", user.isPremium());
        data.put("transactions", txList.stream().map(tx -> {
            Map<String, Object> t = new HashMap<>();
            t.put("id", tx.getId());
            t.put("amount", tx.getAmount());
            t.put("status", tx.getStatus());
            t.put("memo", tx.getMemo());
            t.put("bankRef", tx.getBankRef());
            t.put("createdAt", tx.getCreatedAt());
            t.put("completedAt", tx.getCompletedAt());
            return t;
        }).toList());

        return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", data));
    }

    // ================================================================
    //  POST /api/v1/payment/simulate-success  (ADMIN only)
    // ================================================================
    @PostMapping("/simulate-success")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulatePaymentSuccess(@RequestParam String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        // Ghi transaction COMPLETED
        String memo = memoPrefix + " " + userId + " (SIMULATED)";
        PaymentTransaction tx = PaymentTransaction.builder()
                .userId(userId)
                .amount(paymentAmount)
                .status(TransactionStatus.COMPLETED)
                .memo(memo)
                .bankRef("SIM-" + System.currentTimeMillis())
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        user.setPremium(true);
        userRepository.save(user);

        log.info(">>> SIMULATED PAYMENT SUCCESS. USER UPGRADED: {}", user.getEmail());

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("isPremium", true);
        data.put("transactionId", tx.getId());

        return ResponseEntity.ok(ApiResponse.success("Payment simulated! Premium activated.", data));
    }

    // ================================================================
    //  Helpers
    // ================================================================
    private boolean verifyHmac(String data, String expectedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString().equalsIgnoreCase(expectedSignature);
        } catch (Exception e) {
            log.error("HMAC verification error: {}", e.getMessage());
            return false;
        }
    }

    private String extractMemo(Map<String, Object> data) {
        for (String key : List.of("description", "memo", "content", "addInfo")) {
            if (data.get(key) instanceof String s && !s.isBlank()) return s;
        }
        // SePay nests data inside "data" object
        if (data.get("data") instanceof Map<?, ?> inner) {
            for (String key : List.of("description", "memo", "content")) {
                if (inner.get(key) instanceof String s && !s.isBlank()) return s;
            }
        }
        return null;
    }

    private String extractString(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object val = data.get(key);
            if (val instanceof String s && !s.isBlank()) return s;
            if (val instanceof Number n) return n.toString();
        }
        return null;
    }
}
