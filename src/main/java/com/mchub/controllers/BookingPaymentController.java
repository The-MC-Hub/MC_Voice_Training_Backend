package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.enums.BookingStatus;
import com.mchub.enums.PaymentStatus;
import com.mchub.enums.TransactionStatus;
import com.mchub.enums.TransactionType;
import com.mchub.enums.NotificationType;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Booking;
import com.mchub.models.Transaction;
import com.mchub.repositories.BookingRepository;
import com.mchub.repositories.TransactionRepository;
import com.mchub.services.NotificationService;
import com.mchub.services.PayOSService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1/payment/booking")
@RequiredArgsConstructor
public class BookingPaymentController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookingPaymentController.class);

    private final BookingRepository bookingRepository;
    private final TransactionRepository bookingTransactionRepository;
    private final PayOSService payOSService;
    private final NotificationService notificationService;

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBookingPayment(@RequestParam String bookingId) {
        String userId = SecurityUtils.getCurrentUserId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getMc().equals(userId)) {
            throw new AppException(ErrorCode.BOOKING_ACCESS_DENIED);
        }
        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION, "Booking must be ACCEPTED before payment");
        }
        if (booking.getPaymentStatus() == PaymentStatus.FULLY_PAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PAID);
        }

        double amount = booking.getPrice() <= 0 ? 100000 : booking.getPrice();
        int amountInt = (int) Math.round(amount);
        long orderCode = System.currentTimeMillis() % 1_000_000_000L * 100 + ThreadLocalRandom.current().nextInt(100);

        Map<String, Object> checkout;
        try {
            checkout = payOSService.createBookingPaymentLink(bookingId, orderCode, amountInt);
        } catch (Exception e) {
            log.error("PayOS createBookingPaymentLink failed: {}", SecurityUtils.safeMessage(e));
            throw new AppException(ErrorCode.PAYMENT_INIT_FAILED);
        }

        Transaction tx = Transaction.builder()
                .booking(bookingId)
                .client(booking.getClient())
                .mc(booking.getMc())
                .amount(amountInt)
                .type(TransactionType.FINAL_PAYMENT)
                .status(TransactionStatus.PENDING)
                .payosOrderCode(orderCode)
                .payosPaymentLinkId(String.valueOf(checkout.getOrDefault("paymentLinkId", "")))
                .build();
        bookingTransactionRepository.save(tx);

        Map<String, Object> data = new HashMap<>();
        data.put("bookingId", bookingId);
        data.put("amount", amountInt);
        data.put("orderCode", orderCode);
        data.put("checkoutUrl", String.valueOf(checkout.getOrDefault("checkoutUrl", "")));
        data.put("qrCode", String.valueOf(checkout.getOrDefault("qrCode", "")));

        return ResponseEntity.ok(ApiResponse.success("Booking payment link created", data));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleBookingWebhook(@RequestBody Map<String, Object> webhookBody) {
        log.info(">>> Booking PayOS webhook received");

        if (!payOSService.verifyWebhookSignature(webhookBody)) {
            log.warn(">>> Booking webhook signature invalid");
            return ResponseEntity.ok(Map.of("code", "00", "desc", "success"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) webhookBody.get("data");
        if (data == null) return ResponseEntity.ok(Map.of("code", "00", "desc", "success"));

        String code = String.valueOf(webhookBody.getOrDefault("code", ""));
        if ("00".equals(code)) {
            Object orderCodeObj = data.get("orderCode");
            if (orderCodeObj != null) {
                long orderCode = ((Number) orderCodeObj).longValue();
                bookingTransactionRepository.findByPayosOrderCode(orderCode).ifPresent(tx -> {
                    if (tx.getStatus() == TransactionStatus.PENDING) {
                        tx.setStatus(TransactionStatus.COMPLETED);
                        tx.setPaidAt(LocalDateTime.now());
                        bookingTransactionRepository.save(tx);

                        String bookingId = tx.getBooking();
                        if (bookingId != null) {
                            bookingRepository.findById(bookingId).ifPresent(booking -> {
                                // FIX: set BOTH paymentStatus and status
                                booking.setPaymentStatus(PaymentStatus.FULLY_PAID);
                                booking.setStatus(BookingStatus.PAID);
                                bookingRepository.save(booking);

                                notificationService.notify(booking.getClient(), NotificationType.PAYMENT_SUCCESS,
                                        "Thanh toán thành công",
                                        "Booking \"" + booking.getEventName() + "\" đã được thanh toán.",
                                        "/m/bookings/" + bookingId, true);
                                notificationService.notify(booking.getMc(), NotificationType.PAYMENT_SUCCESS,
                                        "Booking đã thanh toán",
                                        "Booking \"" + booking.getEventName() + "\" đã được thanh toán bởi khách hàng.",
                                        "/m/bookings/" + bookingId, false);
                            });
                        }
                    }
                });
            }
        }

        return ResponseEntity.ok(Map.of("code", "00", "desc", "success"));
    }
}
