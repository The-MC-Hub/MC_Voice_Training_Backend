package com.mchub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchub.config.PlanConfig;
import com.mchub.enums.SubscriptionPlan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class PayOSService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PayOSService.class);

    private static final String PAYOS_API = "https://api-merchant.payos.vn";

    @Value("${mchub.payos.client-id:}")
    private String clientId;

    @Value("${mchub.payos.api-key:}")
    private String apiKey;

    @Value("${mchub.payos.checksum-key:}")
    private String checksumKey;

    @Value("${mchub.fe-url:http://localhost:5173}")
    private String feUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PayOSService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> createPaymentLink(String userId, SubscriptionPlan plan, long orderCode) throws Exception {
        return createPaymentLink(userId, plan, orderCode, PlanConfig.priceFor(plan));
    }

    public Map<String, Object> createPaymentLink(String userId, SubscriptionPlan plan, long orderCode, int amount) throws Exception {
        // PayOS description max 25 chars
        String description = ("MCHUB " + plan.name() + " " + userId);
        if (description.length() > 25) description = description.substring(0, 25);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", "MC Hub " + plan.name());
        item.put("quantity", 1);
        item.put("price", amount);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("items", List.of(item));
        body.put("cancelUrl", feUrl + "/m/payment/cancel");
        body.put("returnUrl", feUrl + "/m/payment/success");

        String signature = createSignature(body);
        body.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                PAYOS_API + "/v2/payment-requests", request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !"00".equals(responseBody.get("code"))) {
            String desc = responseBody != null ? String.valueOf(responseBody.get("desc")) : "null response";
            throw new RuntimeException("PayOS error: " + desc);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        return data;
    }

    public boolean verifyWebhookSignature(Map<String, Object> webhookData) {
        try {
            Object sigObj = webhookData.get("signature");
            if (sigObj == null) return false;
            String receivedSig = sigObj.toString();

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
            if (data == null) return false;

            String computedSig = createSignatureFromData(data);
            return computedSig.equalsIgnoreCase(receivedSig);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    // PayOS signature: sorted key=value pairs joined by &, then HMAC-SHA256
    private String createSignature(Map<String, Object> data) throws Exception {
        Map<String, Object> signable = new TreeMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if ("items".equals(key) || "signature".equals(key)) continue;
            signable.put(key, entry.getValue());
        }
        return hmacSha256(buildQueryString(signable), checksumKey);
    }

    private String createSignatureFromData(Map<String, Object> data) throws Exception {
        Map<String, Object> signable = new TreeMap<>(data);
        signable.remove("signature");
        return hmacSha256(buildQueryString(signable), checksumKey);
    }

    private String buildQueryString(Map<String, Object> sorted) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : sorted.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
