package com.mchub.dto;

import com.mchub.enums.AuditAction;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponseDTO {

    private String id;
    private String userId;
    private AuditAction action;
    private String resource;
    private String resourceId;
    private String details;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;

}
