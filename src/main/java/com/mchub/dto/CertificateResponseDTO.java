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
public class CertificateResponseDTO {
  private String id;
  private String userId;
  private String courseId;
  private String courseName;
  private String userName;
  private double completionScore;
  private String imageUrl;
  private boolean isVerified;
  private LocalDateTime issuedAt;
}
