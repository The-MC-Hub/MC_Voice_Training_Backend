package com.mchub.dto;

import com.mchub.enums.EventType;
import java.util.List;
import lombok.Data;

@Data
public class SearchMCRequest {
  private String keyword;
  private List<EventType> eventTypes;
  private List<String> regions;
  private List<String> styles;
  private List<String> languages;
  private String hostingStyle;
  private Double budgetMin;
  private Double budgetMax;
  private Integer minExperience;
  private String sortBy; // "score" | "rating" | "experience" | "price_low" | "price_high"
}
