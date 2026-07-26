package com.mchub.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLogDTO {
  private String id;
  private String campaignId;
  private String email;
  private String status;
  private String errorReason;
  private LocalDateTime sentAt;
}
