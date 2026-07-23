package com.mchub.models;

import com.mchub.enums.DressCode;
import com.mchub.enums.VenueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "bookingdetails")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetail {

    @Id
    private String id;

    @Indexed(unique = true)
    private String bookingId;

    @Builder.Default
    private DressCode dressCode = DressCode.FORMAL;

    @Builder.Default
    private VenueType venueType = VenueType.INDOOR;

    @Builder.Default
    private boolean hasStage = false;

    @Builder.Default
    private boolean hasMicrophone = true;

    @Builder.Default
    private boolean hasBackgroundMusic = false;

    @Builder.Default
    private boolean hasProjector = false;

    @Builder.Default
    private List<TimelineItem> timeline = new ArrayList<>();

    @Builder.Default
    private List<String> specialGuestNames = new ArrayList<>();

    private String clientNotes;

    private String mcNotes;

    private String venueAddress;

    private String dressCodeDetail;

    @CreatedDate
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineItem {
        private String time;
        private String activity;
        private int durationMinutes;
    }
}
