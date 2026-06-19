package com.mchub.models;

import com.mchub.enums.SubscriptionPlan;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Discount/promo code for payment page.
 * type=PERCENT → discountValue is percentage (e.g. 20 = 20% off)
 * type=FIXED   → discountValue is VND amount subtracted
 */
@Document(collection = "discount_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCode {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code; // e.g. "SUMMER30"

    private DiscountType type; // PERCENT | FIXED

    private int discountValue; // percent (0-100) or VND amount

    // Which plans this code applies to. Empty = all plans.
    private List<SubscriptionPlan> applicablePlans;

    private int maxUses;     // 0 = unlimited
    private int usedCount;

    private LocalDateTime startsAt;   // null = active immediately
    private LocalDateTime expiresAt; // null = never expires

    private boolean active;

    private String description; // admin note

    @CreatedDate
    private LocalDateTime createdAt;

    public enum DiscountType {
        PERCENT, FIXED
    }
}
