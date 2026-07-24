package com.mchub.dto;

import com.mchub.enums.ScheduleStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleResponseDTO {
    private String id;
    private String mc;
    private LocalDate date;
    private String startTime;
    private String endTime;
    private ScheduleStatus status;
    private String bookingId;
    private String note;
    private LocalDateTime createdAt;
}
