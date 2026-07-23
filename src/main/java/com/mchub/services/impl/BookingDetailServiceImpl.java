package com.mchub.services.impl;

import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.BookingDetail;
import com.mchub.repositories.BookingDetailRepository;
import com.mchub.services.BookingDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingDetailServiceImpl implements BookingDetailService {

    private final BookingDetailRepository bookingDetailRepository;

    @Override
    public BookingDetail getOrCreate(String bookingId) {
        return bookingDetailRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    BookingDetail detail = BookingDetail.builder()
                            .bookingId(bookingId)
                            .build();
                    return bookingDetailRepository.save(detail);
                });
    }

    @Override
    public BookingDetail update(String bookingId, BookingDetail updated) {
        BookingDetail detail = bookingDetailRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND));

        if (updated.getDressCode() != null) detail.setDressCode(updated.getDressCode());
        if (updated.getVenueType() != null) detail.setVenueType(updated.getVenueType());
        detail.setHasStage(updated.isHasStage());
        detail.setHasMicrophone(updated.isHasMicrophone());
        detail.setHasBackgroundMusic(updated.isHasBackgroundMusic());
        detail.setHasProjector(updated.isHasProjector());
        if (updated.getTimeline() != null) detail.setTimeline(updated.getTimeline());
        if (updated.getSpecialGuestNames() != null) detail.setSpecialGuestNames(updated.getSpecialGuestNames());
        if (updated.getClientNotes() != null) detail.setClientNotes(updated.getClientNotes());
        if (updated.getMcNotes() != null) detail.setMcNotes(updated.getMcNotes());
        if (updated.getVenueAddress() != null) detail.setVenueAddress(updated.getVenueAddress());
        if (updated.getDressCodeDetail() != null) detail.setDressCodeDetail(updated.getDressCodeDetail());

        return bookingDetailRepository.save(detail);
    }
}
