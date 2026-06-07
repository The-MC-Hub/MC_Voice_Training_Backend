package com.mchub.models;

import com.mchub.enums.SubscriptionPlan;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin-editable plan configuration stored in MongoDB.
 * One document per SubscriptionPlan (BASIC, FULL, ANNUAL).
 */
@Document(collection = "plan_definitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDefinition {

    @Id
    private String id;

    @Indexed(unique = true)
    private SubscriptionPlan plan;

    private int priceVnd;
    private int durationDays;

    // AI session limit per period. -1 = unlimited.
    private int aiSessionLimit;

    // Display info
    private String displayName;
    private String tagline;
    private String description;
    private String badge;
    private String urgencyText;   // e.g. "Chỉ hơn 100đ/ngày"
    private String socialProof;   // e.g. "94% người dùng cải thiện sau 2 tuần"

    // Feature bullets shown on pricing card
    private List<String> highlights;

    // Full comparison row values — key = feature label, value = display string
    private List<ComparisonEntry> comparisonEntries;

    // Direct discount on plan price (0 = no discount).
    // discountPercent is derived; only discountedPriceVnd is authoritative.
    private int discountedPriceVnd;   // 0 means no discount active
    private int discountPercent;      // 0-100, for display only

    private boolean active;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonEntry {
        private String feature;
        private String value; // e.g. "Không giới hạn", "✓", "❌", "50 bài"
    }
}
