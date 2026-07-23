package com.mchub.dto;

import com.mchub.enums.DressCode;
import com.mchub.enums.VenueType;
import com.mchub.models.BookingDetail;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingDetailResponseDTO {
    private String id;
    private String bookingId;
    private DressCode dressCode;
    private VenueType venueType;
    private boolean hasStage;
    private boolean hasMicrophone;
    private boolean hasBackgroundMusic;
    private boolean hasProjector;
    private List<BookingDetail.TimelineItem> timeline;
    private List<String> specialGuestNames;
    private String clientNotes;
    private String mcNotes;
    private String venueAddress;
    private String dressCodeDetail;
    private LocalDateTime createdAt;
}
