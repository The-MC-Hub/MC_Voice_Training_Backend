package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.models.BookingDetail;
import com.mchub.services.BookingDetailService;
import com.mchub.mapper.BookingDetailMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/detail")
@RequiredArgsConstructor
public class BookingDetailController {

    private final BookingDetailService bookingDetailService;
    private final BookingDetailMapper bookingDetailMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<BookingDetailResponseDTO>> getDetail(@PathVariable String bookingId) {
        BookingDetail detail = bookingDetailService.getOrCreate(bookingId);
        return ResponseEntity.ok(ApiResponse.success(bookingDetailMapper.toResponseDTO(detail)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<BookingDetailResponseDTO>> createOrUpdate(
            @PathVariable String bookingId,
            @RequestBody CreateBookingDetailRequest req) {
        BookingDetail detail = BookingDetail.builder()
                .bookingId(bookingId)
                .dressCode(req.getDressCode())
                .venueType(req.getVenueType())
                .hasStage(req.isHasStage())
                .hasMicrophone(req.isHasMicrophone())
                .hasBackgroundMusic(req.isHasBackgroundMusic())
                .hasProjector(req.isHasProjector())
                .timeline(req.getTimeline())
                .specialGuestNames(req.getSpecialGuestNames())
                .clientNotes(req.getClientNotes())
                .venueAddress(req.getVenueAddress())
                .dressCodeDetail(req.getDressCodeDetail())
                .build();
        BookingDetail saved = bookingDetailService.update(bookingId, detail);
        return ResponseEntity.ok(ApiResponse.success("Detail updated successfully", bookingDetailMapper.toResponseDTO(saved)));
    }
}
