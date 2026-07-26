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
public class SocialPostDTO {
  private String id;
  private String image;
  private String description;
  private String fbLink;
  private int sortOrder;
  private boolean active;
  private long clickCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
