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
public class PracticeReviewDTO {
  private String id;
  private String practiceSessionId;
  private String lessonId;
  private String lessonTitle;
  private String revieweeId;
  private String revieweeName;
  private String reviewerId;
  private String reviewerName;
  private String comment;
  private Integer rating;
  private String status;
  private String audioUrl;
  private LocalDateTime createdAt;
  private LocalDateTime reviewedAt;
}
