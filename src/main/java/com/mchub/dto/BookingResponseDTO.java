package com.mchub.dto;

import com.mchub.enums.BookingStatus;
import com.mchub.enums.EventType;
import com.mchub.enums.PaymentStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookingResponseDTO {
    private String id;
    private String client;
    private String mc;
    private String eventName;
    private EventType eventType;
    private LocalDate eventDate;
    private String startTime;
    private String endTime;
    private String location;
    private String description;
    private int audienceSize;
    private double budget;
    private String specialRequests;
    private double price;
    private String rejectionReason;
    private String couponCode;
    private double discountAmount;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;

    private String clientName;
    private String mcName;
    private String clientAvatar;
    private String mcAvatar;

    // MC profile info for clients
    private Double mcRatesMin;
    private Double mcRatesMax;
    private Integer mcExperience;
    private Double mcRating;
    private String mcRegion;
    private String mcEventTypes;
}
