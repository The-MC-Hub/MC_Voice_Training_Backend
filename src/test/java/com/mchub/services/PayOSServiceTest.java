package com.mchub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchub.enums.SubscriptionPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PayOSService — mocks RestTemplate, never calls the real
 * PayOS API. Exercises the DRY refactor (createPaymentLink/createCoursePaymentLink
 * sharing createGenericPaymentLink) and the HMAC-SHA256 webhook signature logic
 * (same algorithm manually replicated during UC-06 System Test Execution).
 */
@ExtendWith(MockitoExtension.class)
class PayOSServiceTest {

    private static final String CHECKSUM_KEY = "test-checksum-key-not-a-real-secret";
    private static final String CLIENT_ID = "test-client-id";
    private static final String API_KEY = "test-api-key";
    private static final String FE_URL = "http://localhost:5173";

    @Mock private RestTemplate restTemplate;

    private PayOSService payOSService;

    @BeforeEach
    void setUp() {
        payOSService = new PayOSService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(payOSService, "clientId", CLIENT_ID);
        ReflectionTestUtils.setField(payOSService, "apiKey", API_KEY);
        ReflectionTestUtils.setField(payOSService, "checksumKey", CHECKSUM_KEY);
        ReflectionTestUtils.setField(payOSService, "feUrl", FE_URL);
    }

    private ResponseEntity<Map> successResponse(String checkoutUrl) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("checkoutUrl", checkoutUrl);
        data.put("qrCode", "00020101...");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "00");
        body.put("desc", "success");
        body.put("data", data);
        return ResponseEntity.ok(body);
    }

    @Nested
    @DisplayName("createPaymentLink / createCoursePaymentLink — shared createGenericPaymentLink")
    class CreatePaymentLink {

        @Test
        @DisplayName("plan subscription link: posts to PayOS with truncated description and item name 'MC Hub <PLAN>'")
        @SuppressWarnings("unchecked")
        void createsPlanPaymentLink() throws Exception {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(successResponse("https://pay.payos.vn/web/abc123"));

            Map<String, Object> result = payOSService.createPaymentLink("user-1", SubscriptionPlan.BASIC, 12345L, 199000);

            assertThat(result).containsEntry("checkoutUrl", "https://pay.payos.vn/web/abc123");
        }

        @Test
        @DisplayName("course purchase link: item name is the course title (truncated to 50 chars), description prefixed 'MCHUB COURSE'")
        void createsCoursePaymentLink() throws Exception {
            ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            when(restTemplate.postForEntity(anyString(), requestCaptor.capture(), eq(Map.class)))
                    .thenReturn(successResponse("https://pay.payos.vn/web/course123"));

            String longTitle = "A".repeat(60);
            payOSService.createCoursePaymentLink("user-1", longTitle, 999L, 199000);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) requestCaptor.getValue().getBody();
            @SuppressWarnings("unchecked")
            var items = (java.util.List<Map<String, Object>>) body.get("items");
            assertThat(items.get(0).get("name")).isEqualTo("A".repeat(50));
            assertThat((String) body.get("description")).startsWith("MCHUB COURSE").hasSizeLessThanOrEqualTo(25);
        }

        @Test
        @DisplayName("description longer than 25 chars is truncated for plan orders")
        void truncatesLongDescription() throws Exception {
            ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            when(restTemplate.postForEntity(anyString(), requestCaptor.capture(), eq(Map.class)))
                    .thenReturn(successResponse("https://pay.payos.vn/web/x"));

            payOSService.createPaymentLink("a-very-long-user-id-that-overflows-25-chars", SubscriptionPlan.ANNUAL, 1L, 100);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) requestCaptor.getValue().getBody();
            assertThat((String) body.get("description")).hasSizeLessThanOrEqualTo(25);
        }

        @Test
        @DisplayName("throws RuntimeException when PayOS responds with a non-'00' code")
        void throwsWhenPayOsReturnsError() {
            Map<String, Object> body = Map.of("code", "01", "desc", "Invalid signature");
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(body));

            assertThatThrownBy(() -> payOSService.createPaymentLink("user-1", SubscriptionPlan.BASIC, 1L, 199000))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PayOS error")
                    .hasMessageContaining("Invalid signature");
        }

        @Test
        @DisplayName("throws RuntimeException when PayOS response body is null")
        void throwsWhenResponseBodyNull() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(null));

            assertThatThrownBy(() -> payOSService.createPaymentLink("user-1", SubscriptionPlan.BASIC, 1L, 199000))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("null response");
        }
    }

    @Nested
    @DisplayName("verifyWebhookSignature")
    class VerifyWebhookSignature {

        private String hmacSha256(String data, String key) throws Exception {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        }

        private Map<String, Object> validWebhookPayload() throws Exception {
            Map<String, Object> data = new TreeMap<>();
            data.put("orderCode", 123456L);
            data.put("amount", 199000);
            data.put("code", "00");
            data.put("desc", "success");

            StringBuilder qs = new StringBuilder();
            for (var e : data.entrySet()) {
                if (qs.length() > 0) qs.append('&');
                qs.append(e.getKey()).append('=').append(e.getValue());
            }
            String signature = hmacSha256(qs.toString(), CHECKSUM_KEY);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("code", "00");
            payload.put("desc", "success");
            payload.put("data", data);
            payload.put("signature", signature);
            return payload;
        }

        @Test
        @DisplayName("returns true for a correctly signed payload")
        void validSignatureReturnsTrue() throws Exception {
            assertThat(payOSService.verifyWebhookSignature(validWebhookPayload())).isTrue();
        }

        @Test
        @DisplayName("returns false when signature does not match computed HMAC")
        void invalidSignatureReturnsFalse() throws Exception {
            Map<String, Object> payload = validWebhookPayload();
            payload.put("signature", "0000000000000000000000000000000000000000000000000000000000000000");

            assertThat(payOSService.verifyWebhookSignature(payload)).isFalse();
        }

        @Test
        @DisplayName("returns false — not an exception — when 'signature' key is missing")
        void missingSignatureReturnsFalse() {
            Map<String, Object> payload = Map.of("code", "00", "data", Map.of("orderCode", 1));

            assertThat(payOSService.verifyWebhookSignature(payload)).isFalse();
        }

        @Test
        @DisplayName("returns false — not an exception — when 'data' key is missing")
        void missingDataReturnsFalse() {
            Map<String, Object> payload = Map.of("code", "00", "signature", "abc");

            assertThat(payOSService.verifyWebhookSignature(payload)).isFalse();
        }

        @Test
        @DisplayName("signature comparison is case-insensitive (PayOS may send upper/lowercase hex)")
        void signatureComparisonIsCaseInsensitive() throws Exception {
            Map<String, Object> payload = validWebhookPayload();
            payload.put("signature", ((String) payload.get("signature")).toUpperCase());

            assertThat(payOSService.verifyWebhookSignature(payload)).isTrue();
        }
    }

    // local static import shim — avoids pulling Mockito's eq() into the wildcard-conflicting
    // ArgumentMatchers import used above for any()/anyString()
    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
