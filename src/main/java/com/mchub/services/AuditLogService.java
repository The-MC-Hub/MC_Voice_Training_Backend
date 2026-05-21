package com.mchub.services;

import com.mchub.enums.AuditAction;
import com.mchub.models.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface AuditLogService {

        void log(String userId, AuditAction action, String resource,
             String resourceId, String details, HttpServletRequest request);

        void logError(String userId, AuditAction action, String resource,
                  String errorMessage, HttpServletRequest request);

    @PreAuthorize("hasAuthority('ADMIN') or #userId == authentication.name")
    List<AuditLog> getUserLogs(String userId);

    @PreAuthorize("hasAuthority('ADMIN')")
    List<AuditLog> getAllLogs();
}
