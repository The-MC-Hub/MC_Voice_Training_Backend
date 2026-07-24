package com.mchub.models;

import com.mchub.enums.EventType;
import com.mchub.enums.MCStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "mcprofiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCProfile {

    @Id
    private String id;

    private String user;

    @Builder.Default
    private Rates rates = new Rates(0, 0);

    @Builder.Default
    private List<EventType> eventTypes = new ArrayList<>();

    @Builder.Default
    private MCStatus status = MCStatus.AVAILABLE;

    @Builder.Default
    private double rating = 0;

    @Builder.Default
    private int reviewsCount = 0;

    @Builder.Default
    private List<String> regions = new ArrayList<>();

    @Builder.Default
    private int experience = 0;

    @Builder.Default
    private List<String> styles = new ArrayList<>();

    @Builder.Default
    private String biography = "";

    @Builder.Default
    private String personality = "";

    @Builder.Default
    private String hostingStyle = "";

    @Builder.Default
    private List<String> notableEvents = new ArrayList<>();

    @Builder.Default
    private List<String> languages = new ArrayList<>();

    // Social links
    @Builder.Default
    private SocialLinks socialLinks = new SocialLinks();

    // Portfolio: name → URL (showreel, highlight video, photo album)
    @Builder.Default
    private Map<String, String> portfolio = new java.util.LinkedHashMap<>();

    // Quick response indicator (minutes)
    @Builder.Default
    private int responseTime = 0;

    // Total events hosted by this MC
    @Builder.Default
    private int totalEvents = 0;

    // Achievements / certifications display strings
    @Builder.Default
    private List<String> achievements = new ArrayList<>();

    // Preferred contact method
    @Builder.Default
    private String preferredContact = "IN_APP"; // IN_APP, PHONE, EMAIL, ZALO

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder.Default
    private int searchCount = 0;

    private LocalDateTime lastActive;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rates {
        private double min;
        private double max;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialLinks {
        private String youtube;
        private String tiktok;
        private String facebook;
        private String zalo;
    }
}
