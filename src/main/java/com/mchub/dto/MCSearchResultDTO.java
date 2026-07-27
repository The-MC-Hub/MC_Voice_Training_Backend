package com.mchub.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MCSearchResultDTO {
  private MCProfileResponseDTO profile;
  private double score;
  private Map<String, Object> scoreBreakdown;
}
