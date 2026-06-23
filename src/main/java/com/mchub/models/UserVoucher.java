package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "user_vouchers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVoucher {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String code;            // maps to DiscountCode.code
    private int discountPercent;    // cached for display
    private String description;     // e.g. "Hoàn thành nhiệm vụ tân binh"
    private String source;          // NEWBIE_QUEST | REFERRAL | ADMIN | EVENT

    @Builder.Default
    private boolean active = true;

    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;   // null = not yet used

    @CreatedDate
    private LocalDateTime createdAt;
}
