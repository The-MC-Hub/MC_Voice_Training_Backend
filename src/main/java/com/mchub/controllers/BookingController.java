package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Booking;
import com.mchub.models.MCProfile;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.BookingService;
import com.mchub.mapper.BookingMapper;
import com.mchub.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final MCProfileRepository mcProfileRepository;
    private final BookingMapper bookingMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponseDTO>> createBooking(@RequestBody @Valid CreateBookingRequest req) {
        String clientId = SecurityUtils.getCurrentUserId();
        Booking saved = bookingService.createBooking(req, clientId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", enrichWithUserInfo(bookingMapper.toResponseDTO(saved))));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponseDTO>>> getMyBookings(@RequestParam(required = false) String role) {
        String userId = SecurityUtils.getCurrentUserId();
        List<Booking> list = "mc".equalsIgnoreCase(role)
                ? bookingService.getMCBookings(userId)
                : bookingService.getClientBookings(userId);
        List<BookingResponseDTO> dtos = list.stream()
                .map(bookingMapper::toResponseDTO)
                .map(this::enrichWithUserInfo)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> getBookingById(@PathVariable String id) {
        Booking b = bookingService.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.success(enrichWithUserInfo(bookingMapper.toResponseDTO(b))));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> updateStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateBookingStatusRequest req) {
        String mcId = SecurityUtils.getCurrentUserId();
        Booking updated = bookingService.updateStatus(id, req.getStatus(), mcId, req.getPrice(), req.getRejectionReason());
        return ResponseEntity.ok(ApiResponse.success("Update successful", enrichWithUserInfo(bookingMapper.toResponseDTO(updated))));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> cancelBooking(@PathVariable String id) {
        String clientId = SecurityUtils.getCurrentUserId();
        Booking cancelled = bookingService.cancelBooking(id, clientId);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", enrichWithUserInfo(bookingMapper.toResponseDTO(cancelled))));
    }

    private BookingResponseDTO enrichWithUserInfo(BookingResponseDTO dto) {
        if (dto.getClient() != null) {
            userRepository.findById(dto.getClient()).ifPresent(client -> {
                dto.setClientName(client.getName());
                dto.setClientAvatar(client.getAvatar());
            });
        }
        if (dto.getMc() != null) {
            userRepository.findById(dto.getMc()).ifPresent(mc -> {
                dto.setMcName(mc.getName());
                dto.setMcAvatar(mc.getAvatar());
            });
            mcProfileRepository.findByUser(dto.getMc()).ifPresent(profile -> {
                if (profile.getRates() != null) {
                    dto.setMcRatesMin(profile.getRates().getMin());
                    dto.setMcRatesMax(profile.getRates().getMax());
                }
                dto.setMcExperience(profile.getExperience());
                dto.setMcRating(profile.getRating());
                if (profile.getRegions() != null && !profile.getRegions().isEmpty()) {
                    dto.setMcRegion(profile.getRegions().get(0));
                }
                if (profile.getEventTypes() != null && !profile.getEventTypes().isEmpty()) {
                    dto.setMcEventTypes(profile.getEventTypes().stream()
                            .map(Enum::name)
                            .collect(java.util.stream.Collectors.joining(", ")));
                }
            });
        }
        return dto;
    }
}
