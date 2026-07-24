package com.mchub.models;

import com.mchub.enums.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    private String id;

    @Indexed
    private String mc;

    private LocalDate date;

    private String startTime;

    private String endTime;

    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.UNAVAILABLE;

    private String bookingId;

    private String note;

    @CreatedDate
    private LocalDateTime createdAt;
}
