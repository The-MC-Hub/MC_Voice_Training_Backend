package com.mchub.controllers;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.TransactionStatus;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.Course;
import com.mchub.models.PaymentTransaction;
import com.mchub.models.PlanDefinition;
import com.mchub.models.User;
import com.mchub.repositories.CourseEnrollmentRepository;
import com.mchub.repositories.CourseRepository;
import com.mchub.repositories.PaymentTransactionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.PayOSService;
import com.mchub.services.PlanService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest for PaymentController. SecurityUtils.getCurrentUserId() is
 * exercised with a real Authentication populated in SecurityContextHolder
 * per-test (security filter chain itself is disabled via addFilters=false —
 * /simulate-success and /admin/complete/** have @PreAuthorize but method
 * security is not active in a pure @WebMvcTest slice without
 * @EnableMethodSecurity context, so those endpoints are reachable here
 * regardless of role; that authorization boundary is covered instead by
 * DEFECT-001-style whitelist checks and manual UC-06 system testing).
 */
@WebMvcTest(controllers = PaymentController.class)
@ContextConfiguration(classes = {PaymentController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UserRepository userRepository;
    @MockBean private PaymentTransactionRepository transactionRepository;
    @MockBean private PayOSService payOSService;
    @MockBean private PlanService planService;
    @MockBean private CourseRepository courseRepository;
    @MockBean private CourseEnrollmentRepository courseEnrollmentRepository;
    @MockBean private com.mchub.services.NotificationService notificationService;

    private static final String USER_ID = "user-pay-001";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User baseUser() {
        return User.builder().id(USER_ID).email("qa@test.local").plan(SubscriptionPlan.FREE).build();
    }

    @Nested
    @DisplayName("GET /api/v1/payment/plans, /flash-deals — public, no auth")
    class PublicEndpoints {

        @Test
        @DisplayName("plans returns active plans list")
        void returnsActivePlans() throws Exception {
            when(planService.getActivePlans()).thenReturn(List.of(PlanDefinition.builder().id("p1").build()));

            mockMvc.perform(get("/api/v1/payment/plans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value("p1"));
        }

        @Test
        @DisplayName("flash-deals returns active discount codes")
        void returnsFlashDeals() throws Exception {
            when(planService.getActiveFlashDeals()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/payment/flash-deals"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payment/create-order")
    class CreateOrder {

        @Test
        @DisplayName("400 VALIDATION_FAILED when plan=FREE")
        void rejectsFreePlan() throws Exception {
            mockMvc.perform(post("/api/v1/payment/create-order").param("plan", "FREE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.getCode()));
        }

        @Test
        @DisplayName("404 USER_NOT_FOUND when caller's user record is missing")
        void returns404WhenUserMissing() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/payment/create-order").param("plan", "BASIC"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("100%% discount: activates plan immediately without calling PayOS")
        void activatesImmediatelyOn100PercentDiscount() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(baseUser()));
            when(planService.getPlanByKey(SubscriptionPlan.BASIC))
                    .thenReturn(PlanDefinition.builder().priceVnd(199000).build());
            when(planService.applyDiscount(eq("FREE100"), eq(SubscriptionPlan.BASIC)))
                    .thenReturn(Map.of("finalPrice", 0));
            when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/payment/create-order")
                            .param("plan", "BASIC").param("discountCode", "FREE100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isPremium").value(true))
                    .andExpect(jsonPath("$.data.amount").value(0));

            verify(payOSService, never()).createPaymentLink(anyString(), any(), anyLong(), org.mockito.ArgumentMatchers.anyInt());
            verify(planService).consumeDiscount("FREE100");
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED when discount code is invalid")
        void rejectsInvalidDiscountCode() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(baseUser()));
            when(planService.getPlanByKey(SubscriptionPlan.BASIC))
                    .thenReturn(PlanDefinition.builder().priceVnd(199000).build());
            when(planService.applyDiscount(eq("BADCODE"), eq(SubscriptionPlan.BASIC)))
                    .thenThrow(new AppException(ErrorCode.VALIDATION_FAILED, "Ma giam gia khong ton tai"));

            mockMvc.perform(post("/api/v1/payment/create-order")
                            .param("plan", "BASIC").param("discountCode", "BADCODE"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("creates PENDING transaction and returns checkoutUrl for a normal-price order")
        void createsPendingTransactionWithCheckoutUrl() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(baseUser()));
            when(planService.getPlanByKey(SubscriptionPlan.BASIC))
                    .thenReturn(PlanDefinition.builder().priceVnd(199000).build());
            when(payOSService.createPaymentLink(eq(USER_ID), eq(SubscriptionPlan.BASIC), anyLong(), eq(199000)))
                    .thenReturn(Map.of("checkoutUrl", "https://pay.payos.vn/x", "qrCode", "qr-data"));
            when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/payment/create-order").param("plan", "BASIC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.checkoutUrl").value("https://pay.payos.vn/x"))
                    .andExpect(jsonPath("$.data.amount").value(199000));

            verify(transactionRepository).save(org.mockito.ArgumentMatchers.argThat(
                    tx -> tx.getStatus() == TransactionStatus.PENDING));
        }

        @Test
        @DisplayName("502-mapped INTERNAL_ERROR when PayOS call throws")
        void returnsErrorWhenPayOsFails() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(baseUser()));
            when(planService.getPlanByKey(SubscriptionPlan.BASIC))
                    .thenReturn(PlanDefinition.builder().priceVnd(199000).build());
            when(payOSService.createPaymentLink(anyString(), any(), anyLong(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenThrow(new RuntimeException("PayOS API down"));

            mockMvc.perform(post("/api/v1/payment/create-order").param("plan", "BASIC"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.INTERNAL_ERROR.getCode()));

            verify(transactionRepository, never()).save(any(PaymentTransaction.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payment/course-order")
    class CreateCourseOrder {

        @Test
        @DisplayName("400 VALIDATION_FAILED when course already purchased")
        void rejectsAlreadyPurchasedCourse() throws Exception {
            User user = baseUser();
            user.setPurchasedCourseIds(List.of("course-1"));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(courseRepository.findById("course-1")).thenReturn(Optional.of(Course.builder().id("course-1").build()));

            mockMvc.perform(post("/api/v1/payment/course-order").param("courseId", "course-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED when a discountCode is supplied — not allowed for course orders")
        void rejectsDiscountCodeForCourseOrder() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(baseUser()));
            when(courseRepository.findById("course-1"))
                    .thenReturn(Optional.of(Course.builder().id("course-1").priceVnd(199000).discountPercent(0).build()));

            mockMvc.perform(post("/api/v1/payment/course-order")
                            .param("courseId", "course-1").param("discountCode", "ANY"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 COURSE_NOT_FOUND for unknown courseId")
        void returns404WhenCourseMissing() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(baseUser()));
            when(courseRepository.findById("missing")).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/payment/course-order").param("courseId", "missing"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("applies course discountPercent to compute effectiveAmount")
        void appliesCourseDiscountPercent() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(baseUser()));
            when(courseRepository.findById("course-1")).thenReturn(Optional.of(
                    Course.builder().id("course-1").title("Course").slug("course").priceVnd(200000).discountPercent(50).build()));
            when(payOSService.createCoursePaymentLink(eq(USER_ID), eq("Course"), anyLong(), eq(100000)))
                    .thenReturn(Map.of("checkoutUrl", "https://pay.payos.vn/course"));
            when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/payment/course-order").param("courseId", "course-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.amount").value(100000));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payment/webhook — signature verification + idempotency")
    class Webhook {

        @Test
        @DisplayName("returns 200 without processing when signature is invalid")
        void returns200SilentlyOnInvalidSignature() throws Exception {
            when(payOSService.verifyWebhookSignature(any())).thenReturn(false);

            mockMvc.perform(post("/api/v1/payment/webhook")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"00\",\"data\":{\"orderCode\":123}}"))
                    .andExpect(status().isOk());

            verify(transactionRepository, never()).findByOrderCode(anyLong());
        }

        @Test
        @DisplayName("completes a PENDING transaction and upgrades user plan on valid webhook")
        void completesTransactionAndUpgradesUser() throws Exception {
            when(payOSService.verifyWebhookSignature(any())).thenReturn(true);
            PaymentTransaction tx = PaymentTransaction.builder().id("tx-1").userId(USER_ID)
                    .plan(SubscriptionPlan.BASIC).status(TransactionStatus.PENDING).orderCode(123L).build();
            when(transactionRepository.findByOrderCode(123L)).thenReturn(Optional.of(tx));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(baseUser()));

            mockMvc.perform(post("/api/v1/payment/webhook")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"00\",\"data\":{\"orderCode\":123,\"reference\":\"REF1\"}}"))
                    .andExpect(status().isOk());

            org.mockito.Mockito.verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(
                    u -> u.isPremium() && u.getPlan() == SubscriptionPlan.BASIC));
        }

        @Test
        @DisplayName("skips processing (idempotent) when transaction is already COMPLETED — DEFECT-002 guard")
        void skipsAlreadyCompletedTransaction() throws Exception {
            when(payOSService.verifyWebhookSignature(any())).thenReturn(true);
            PaymentTransaction tx = PaymentTransaction.builder().id("tx-1").userId(USER_ID)
                    .plan(SubscriptionPlan.BASIC).status(TransactionStatus.COMPLETED).orderCode(123L).build();
            when(transactionRepository.findByOrderCode(123L)).thenReturn(Optional.of(tx));

            mockMvc.perform(post("/api/v1/payment/webhook")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"00\",\"data\":{\"orderCode\":123}}"))
                    .andExpect(status().isOk());

            verify(userRepository, never()).findById(USER_ID);
            verify(transactionRepository, never()).save(any(PaymentTransaction.class));
        }

        @Test
        @DisplayName("grants course access (not plan upgrade) when transaction has a courseId")
        void grantsCoursePurchaseForCourseTransaction() throws Exception {
            when(payOSService.verifyWebhookSignature(any())).thenReturn(true);
            PaymentTransaction tx = PaymentTransaction.builder().id("tx-1").userId(USER_ID)
                    .courseId("course-1").status(TransactionStatus.PENDING).orderCode(123L).build();
            when(transactionRepository.findByOrderCode(123L)).thenReturn(Optional.of(tx));
            User user = baseUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(courseEnrollmentRepository.existsByUserIdAndCourseId(USER_ID, "course-1")).thenReturn(false);

            mockMvc.perform(post("/api/v1/payment/webhook")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"00\",\"data\":{\"orderCode\":123}}"))
                    .andExpect(status().isOk());

            verify(courseEnrollmentRepository).save(any());
        }

        @Test
        @DisplayName("returns 200 and does nothing when data field is missing")
        void returns200WhenDataMissing() throws Exception {
            when(payOSService.verifyWebhookSignature(any())).thenReturn(true);

            mockMvc.perform(post("/api/v1/payment/webhook")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"00\"}"))
                    .andExpect(status().isOk());

            verify(transactionRepository, never()).findByOrderCode(anyLong());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payment/status/{userId} — IDOR guard")
    class GetPaymentStatus {

        @Test
        @DisplayName("403 ACCESS_DENIED when requesting another user's status")
        void rejectsAccessToAnotherUsersStatus() throws Exception {
            mockMvc.perform(get("/api/v1/payment/status/{userId}", "other-user-id"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.ACCESS_DENIED.getCode()));

            verify(userRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("200 OK when requesting own status")
        void allowsAccessToOwnStatus() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(baseUser()));
            when(transactionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/payment/status/{userId}", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isPremium").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payment/simulate-success")
    class SimulateSuccess {

        @Test
        @DisplayName("400 VALIDATION_FAILED when plan=FREE")
        void rejectsFreePlan() throws Exception {
            mockMvc.perform(post("/api/v1/payment/simulate-success")
                            .param("userId", USER_ID).param("plan", "FREE"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("activates plan and marks transaction COMPLETED immediately")
        void activatesPlanImmediately() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(baseUser()));
            when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/payment/simulate-success")
                            .param("userId", USER_ID).param("plan", "BASIC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isPremium").value(true));

            verify(transactionRepository).save(org.mockito.ArgumentMatchers.argThat(
                    tx -> tx.getStatus() == TransactionStatus.COMPLETED));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payment/admin/complete/{transactionId}")
    class AdminCompleteTransaction {

        @Test
        @DisplayName("404 TRANSACTION_NOT_FOUND for unknown transaction id")
        void returns404WhenTransactionMissing() throws Exception {
            when(transactionRepository.findById("missing")).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/payment/admin/complete/{id}", "missing"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED when transaction is already COMPLETED")
        void rejectsAlreadyCompletedTransaction() throws Exception {
            PaymentTransaction tx = PaymentTransaction.builder().id("tx-1").userId(USER_ID)
                    .status(TransactionStatus.COMPLETED).build();
            when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(tx));

            mockMvc.perform(post("/api/v1/payment/admin/complete/{id}", "tx-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("completes a PENDING transaction and upgrades the user's plan")
        void completesAndUpgradesUser() throws Exception {
            PaymentTransaction tx = PaymentTransaction.builder().id("tx-1").userId(USER_ID)
                    .plan(SubscriptionPlan.FULL).status(TransactionStatus.PENDING).build();
            when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(tx));
            when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(baseUser()));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/payment/admin/complete/{id}", "tx-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.plan").value("FULL"));
        }
    }
}
