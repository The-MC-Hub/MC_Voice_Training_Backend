package com.mchub.models;

import com.mchub.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "payment_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    private String id;

    @Indexed
    private String userId;

    private int amount;

    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    private String memo;

    private String bankRef;

    private String webhookRaw;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
