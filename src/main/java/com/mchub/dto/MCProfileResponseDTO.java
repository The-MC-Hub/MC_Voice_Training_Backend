package com.mchub.dto;

import com.mchub.enums.EventType;
import com.mchub.enums.MCStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class MCProfileResponseDTO {
    private String id;
    private String userId;
    private String name;
    private String avatar;
    private boolean isVerified;
    private int experience;
    private List<String> styles;
    private String biography;

    private Double ratesMin;
    private Double ratesMax;
    private List<EventType> eventTypes;
    private MCStatus status;
    private Double rating;
    private Integer reviewsCount;
    private List<String> regions;
    private String personality;
    private String hostingStyle;
    private List<String> notableEvents;
    private List<String> languages;
    private int searchCount;
    private String lastActive;

    // New enriched fields
    private SocialLinksDTO socialLinks;
    private Map<String, String> portfolio;
    private int responseTime;
    private int totalEvents;
    private List<String> achievements;
    private String preferredContact;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialLinksDTO {
        private String youtube;
        private String tiktok;
        private String facebook;
        private String zalo;
    }
}
