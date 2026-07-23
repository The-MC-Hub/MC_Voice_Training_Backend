package com.mchub.models;

import com.mchub.enums.TransactionStatus;
import com.mchub.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    private String id;

    private String booking;

    private String client;

    private String mc;

    private double amount;

    private TransactionType type;

    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Builder.Default
    private double platformFee = 0.0;

    private String transactionId;

    private Long payosOrderCode;

    private String payosPaymentLinkId;

    private LocalDateTime paidAt;

    private String note;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
