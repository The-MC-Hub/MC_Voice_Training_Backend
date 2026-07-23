package com.mchub.models;

import com.mchub.enums.BookingStatus;
import com.mchub.enums.EventType;
import com.mchub.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    private String id;

    private String client;

    private String mc;

    private LocalDate eventDate;

    private String eventName;

    private String startTime;

    private String endTime;

    private String location;

    private EventType eventType;

    private String description;

    @Builder.Default
    private int audienceSize = 0;

    private double budget;

    private String specialRequests;

    private double price;

    private String rejectionReason;

    private String couponCode;

    @Builder.Default
    private double discountAmount = 0;

    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDateTime decidedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
