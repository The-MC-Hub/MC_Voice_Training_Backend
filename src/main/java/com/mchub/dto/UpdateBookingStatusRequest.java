package com.mchub.dto;

import com.mchub.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateBookingStatusRequest {

    @NotNull
    private BookingStatus status;

    private Double price;
    private String rejectionReason;
}
