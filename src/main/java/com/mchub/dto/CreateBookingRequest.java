package com.mchub.dto;

import com.mchub.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class CreateBookingRequest {

    @NotBlank
    private String mc;

    @NotNull
    private LocalDate eventDate;

    @NotBlank
    private String eventName;

    @NotBlank
    private String startTime;

    @NotBlank
    private String endTime;

    @NotBlank
    private String location;

    @NotNull
    private EventType eventType;

    private String description;

    @Positive
    private int audienceSize;

    @Positive
    private double budget;

    private String specialRequests;
    private String couponCode;
}
