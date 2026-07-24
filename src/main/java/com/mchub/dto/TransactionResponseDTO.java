package com.mchub.dto;

import com.mchub.enums.TransactionStatus;
import com.mchub.enums.TransactionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionResponseDTO {
    private String id;
    private String booking;
    private String client;
    private String mc;
    private int amount;
    private TransactionType type;
    private TransactionStatus status;
    private int platformFee;
    private String payosOrderCode;
    private String payosPaymentLinkId;
    private LocalDateTime paidAt;
    private String note;
    private LocalDateTime createdAt;
}
