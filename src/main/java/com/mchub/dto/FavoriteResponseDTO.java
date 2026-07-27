package com.mchub.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FavoriteResponseDTO {
  private String id;
  private String clientId;
  private String mcUserId;
  private LocalDateTime createdAt;
}
