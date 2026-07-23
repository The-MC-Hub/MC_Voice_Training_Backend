package com.mchub.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class MCSearchResultDTO {
    private MCProfileResponseDTO profile;
    private double score;
    private Map<String, Object> scoreBreakdown;
}
