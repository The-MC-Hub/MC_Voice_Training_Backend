package com.mchub.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReviewResponseDTO {
  private String id;
  private String bookingId;
  private String mc;
  private String client;
  private int rating;
  private String comment;
  private LocalDateTime createdAt;

  private String clientName;
  private String clientAvatar;
}
