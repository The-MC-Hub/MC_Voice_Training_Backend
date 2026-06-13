package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.services.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/contact")
@RequiredArgsConstructor
public class ContactController {

    private final EmailService emailService;

    @Value("${app.mail.from:themchubtraining@gmail.com}")
    private String supportEmail;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> sendContact(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String name    = body.getOrDefault("name", "").trim();
        String email   = body.getOrDefault("email", "").trim();
        String message = body.getOrDefault("message", "").trim();

        if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Vui lòng điền đầy đủ thông tin");
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Email không hợp lệ");
        }
        if (message.length() > 2000) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Tin nhắn không được vượt quá 2000 ký tự");
        }

        String subject = "[MCHub Contact] Tin nhắn từ " + name;
        String content = "Người gửi: " + name + "\nEmail: " + email + "\n\n" + message;
        emailService.sendSimpleEmail(supportEmail, subject, content);

        return ResponseEntity.ok(ApiResponse.success("Tin nhắn đã được gửi thành công", null));
    }
}
