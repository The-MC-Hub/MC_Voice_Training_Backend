package com.mchub.controllers;

import com.mchub.config.PlanConfig;
import com.mchub.dto.ApiResponse;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.TransactionStatus;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.PaymentTransaction;
import com.mchub.models.PlanDefinition;
import com.mchub.models.User;
import com.mchub.repositories.PaymentTransactionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.PayOSService;
import com.mchub.services.PlanService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentController.class);

    private final UserRepository userRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PayOSService payOSService;
    private final PlanService planService;

    // ================================================================
    //  GET /api/v1/payment/plans  (public — frontend fetches pricing)
    // ================================================================
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<java.util.List<PlanDefinition>>> getActivePlans() {
        return ResponseEntity.ok(ApiResponse.success("Plans retrieved", planService.getActivePlans()));
    }

    // ================================================================
    //  POST /api/v1/payment/apply-discount
    //  Validate discount code, return final price (no side effects)
    // ================================================================
    @PostMapping("/apply-discount")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> applyDiscount(
            @RequestParam String code,
            @RequestParam SubscriptionPlan plan) {
        return ResponseEntity.ok(ApiResponse.success("Discount applied", planService.applyDiscount(code, plan)));
    }

    // ================================================================
    //  POST /api/v1/payment/create-order
    //  Tạo PayOS payment link, trả về checkoutUrl
    // ================================================================
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPremiumOrder(
            @RequestParam String userId,
            @RequestParam(defaultValue = "FULL") SubscriptionPlan plan) {

        if (plan == SubscriptionPlan.FREE) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cannot purchase FREE plan");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));

        // Use discounted price if admin has set one, otherwise fall back to PlanConfig
        int effectiveAmount;
        try {
            PlanDefinition planDef = planService.getPlanByKey(plan);
            effectiveAmount = planDef.getDiscountedPriceVnd() > 0
                    ? planDef.getDiscountedPriceVnd()
                    : planDef.getPriceVnd();
        } catch (Exception e) {
            effectiveAmount = PlanConfig.priceFor(plan);
        }

        // Generate unique orderCode (PayOS requires positive long ≤ 9007199254740991)
        long orderCode = System.currentTimeMillis() % 1_000_000_000L * 100 + ThreadLocalRandom.current().nextInt(100);

        Map<String, Object> checkout;
        try {
            checkout = payOSService.createPaymentLink(userId, plan, orderCode, effectiveAmount);
        } catch (Exception e) {
            log.error("PayOS createPaymentLink failed: {}", SecurityUtils.safeMessage(e));
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Payment service unavailable");
        }

        PaymentTransaction tx = PaymentTransaction.builder()
                .userId(userId)
                .plan(plan)
                .amount(effectiveAmount)
                .status(TransactionStatus.PENDING)
                .orderCode(orderCode)
                .checkoutUrl(String.valueOf(checkout.getOrDefault("checkoutUrl", "")))
                .memo("MCHUB " + plan.name() + " " + userId)
                .build();
        transactionRepository.save(tx);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("plan", plan.name());
        data.put("amount", effectiveAmount);
        data.put("orderCode", orderCode);
        data.put("checkoutUrl", String.valueOf(checkout.getOrDefault("checkoutUrl", "")));
        data.put("qrCode", String.valueOf(checkout.getOrDefault("qrCode", "")));

        return ResponseEntity.ok(ApiResponse.success("Payment link created", data));
    }

    // ================================================================
    //  POST /api/v1/payment/webhook
    //  Nhận webhook từ PayOS — verify HMAC-SHA256
    // ================================================================
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handlePaymentWebhook(@RequestBody Map<String, Object> webhookBody) {
        log.info(">>> PayOS webhook received");

        if (!payOSService.verifyWebhookSignature(webhookBody)) {
            log.warn(">>> Webhook signature invalid");
            return ResponseEntity.ok(Map.of("code", "00", "desc", "success"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) webhookBody.get("data");
        if (data == null) return ResponseEntity.ok(Map.of("code", "00", "desc", "success"));

        String code = String.valueOf(webhookBody.getOrDefault("code", ""));
        log.info(">>> Webhook code={}, orderCode={}", code, data.get("orderCode"));

        if ("00".equals(code)) {
            Object orderCodeObj = data.get("orderCode");
            if (orderCodeObj != null) {
                long orderCode = ((Number) orderCodeObj).longValue();
                transactionRepository.findByOrderCode(orderCode).ifPresent(tx -> {
                    if (tx.getStatus() == TransactionStatus.PENDING) {
                        tx.setStatus(TransactionStatus.COMPLETED);
                        tx.setBankRef(String.valueOf(data.getOrDefault("reference", "")));
                        tx.setCompletedAt(LocalDateTime.now());
                        transactionRepository.save(tx);

                        userRepository.findById(tx.getUserId()).ifPresent(user -> {
                            user.setPremium(true);
                            user.setPlan(tx.getPlan());
                            user.setPlanExpiresAt(LocalDateTime.now().plusDays(PlanConfig.daysFor(tx.getPlan())));
                            user.setAiSessionsUsed(0);
                            userRepository.save(user);
                            log.info(">>> USER UPGRADED: {} → plan={}", user.getEmail(), tx.getPlan());
                        });
                    }
                });
            }
        }

        return ResponseEntity.ok(Map.of("code", "00", "desc", "success"));
    }

    // ================================================================
    //  GET /api/v1/payment/status/{userId}
    //  Frontend polling — kiểm tra trạng thái premium
    // ================================================================
    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentStatus(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));

        List<PaymentTransaction> txList = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("isPremium", user.isPremium());
        data.put("plan", user.getPlan());
        data.put("planExpiresAt", user.getPlanExpiresAt());
        data.put("transactions", txList.stream().map(tx -> {
            Map<String, Object> t = new HashMap<>();
            t.put("id", tx.getId());
            t.put("amount", tx.getAmount());
            t.put("status", tx.getStatus());
            t.put("plan", tx.getPlan());
            t.put("orderCode", tx.getOrderCode());
            t.put("bankRef", tx.getBankRef());
            t.put("createdAt", tx.getCreatedAt());
            t.put("completedAt", tx.getCompletedAt());
            return t;
        }).toList());

        return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", data));
    }

    // ================================================================
    //  POST /api/v1/payment/simulate-success  (dev only)
    // ================================================================
    @PostMapping("/simulate-success")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulatePaymentSuccess(
            @RequestParam String userId,
            @RequestParam(defaultValue = "FULL") SubscriptionPlan plan) {

        if (plan == SubscriptionPlan.FREE) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cannot simulate FREE plan");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));

        long orderCode = System.currentTimeMillis();
        PaymentTransaction tx = PaymentTransaction.builder()
                .userId(userId)
                .plan(plan)
                .amount(PlanConfig.priceFor(plan))
                .status(TransactionStatus.COMPLETED)
                .orderCode(orderCode)
                .memo("SIMULATED " + plan.name() + " " + userId)
                .bankRef("SIM-" + orderCode)
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        user.setPremium(true);
        user.setPlan(plan);
        user.setPlanExpiresAt(LocalDateTime.now().plusDays(PlanConfig.daysFor(plan)));
        user.setAiSessionsUsed(0);
        userRepository.save(user);

        log.info(">>> SIMULATED PAYMENT SUCCESS. USER={} plan={}", user.getEmail(), plan);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("plan", plan.name());
        data.put("isPremium", true);
        data.put("planExpiresAt", user.getPlanExpiresAt());

        return ResponseEntity.ok(ApiResponse.success("Payment simulated! Plan activated.", data));
    }
}
