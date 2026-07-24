package com.mchub.dto;

import com.mchub.enums.DressCode;
import com.mchub.enums.VenueType;
import com.mchub.models.BookingDetail;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateBookingDetailRequest {
    private DressCode dressCode;
    private VenueType venueType;
    private boolean hasStage;
    private boolean hasMicrophone;
    private boolean hasBackgroundMusic;
    private boolean hasProjector;
    private List<BookingDetail.TimelineItem> timeline;
    private List<String> specialGuestNames;
    private String clientNotes;
    private String venueAddress;
    private String dressCodeDetail;
}
