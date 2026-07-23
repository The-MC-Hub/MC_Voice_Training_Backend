package com.mchub.services;

import com.mchub.dto.CreateBookingRequest;
import com.mchub.enums.BookingStatus;
import com.mchub.models.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingService {

    Booking createBooking(CreateBookingRequest req, String clientId);

    Booking updateStatus(String bookingId, BookingStatus newStatus, String mcId, Double price, String rejectionReason);

    Booking cancelBooking(String bookingId, String clientId);

    Optional<Booking> findById(String id);

    List<Booking> getClientBookings(String clientId);

    List<Booking> getMCBookings(String mcId);

    List<Booking> getAllBookings();

    void sendBookingNotifications(Booking booking);

    void initializeChatRoom(Booking booking);
}
