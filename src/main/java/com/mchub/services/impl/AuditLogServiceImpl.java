package com.mchub.services.impl;

import com.mchub.enums.AuditAction;
import com.mchub.models.AuditLog;
import com.mchub.repositories.AuditLogRepository;
import com.mchub.services.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    public void log(String userId, AuditAction action, String resource,
                    String resourceId, String details, HttpServletRequest request) {
        AuditLog log = AuditLog.builder()
            .userId(userId)
            .action(action)
            .resource(resource)
            .resourceId(resourceId)
            .details(details)
            .ipAddress(getClientIp(request))
            .userAgent(request != null ? request.getHeader("User-Agent") : null)
            .status("SUCCESS")
            .build();
        auditLogRepository.save(Objects.requireNonNull(log));
    }

    @Override
    @Async
    public void logError(String userId, AuditAction action, String resource,
                         String errorMessage, HttpServletRequest request) {
        AuditLog log = AuditLog.builder()
            .userId(userId)
            .action(action)
            .resource(resource)
            .ipAddress(getClientIp(request))
            .status("FAILED")
            .errorMessage(errorMessage)
            .build();
        auditLogRepository.save(Objects.requireNonNull(log));
    }

    @Override
    public List<AuditLog> getUserLogs(String userId) {
        return auditLogRepository.findByUserId(userId);
    }

    @Override
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xfHeader = request.getHeader("X-Forwarded-For");
        return (xfHeader != null) ? xfHeader.split(",")[0].trim() : request.getRemoteAddr();
    }
}
