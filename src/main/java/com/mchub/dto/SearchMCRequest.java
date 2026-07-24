package com.mchub.dto;

import com.mchub.enums.EventType;
import lombok.Data;

import java.util.List;

@Data
public class SearchMCRequest {
    private String keyword;
    private List<EventType> eventTypes;
    private List<String> regions;
    private Double budgetMin;
    private Double budgetMax;
    private Integer minExperience;
    private String sortBy; // "score" | "rating" | "experience" | "price_low" | "price_high"
}
