package com.mchub.dto;

import com.mchub.enums.ReportReason;
import com.mchub.enums.ReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ReportResponseDTO {

  private String id;
  private String reporterId;
  private String reportedId;
  private String bookingId;
  private ReportReason reason;
  private String description;
  private List<String> evidenceUrls;
  private ReportStatus status;
  private String adminNote;
  private LocalDateTime resolvedAt;
  private LocalDateTime createdAt;

  private String reporterName;
  private String reportedName;
}
