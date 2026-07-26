package com.mchub.dto;

import com.mchub.enums.AuditAction;
import java.time.LocalDateTime;
import lombok.Data;

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
