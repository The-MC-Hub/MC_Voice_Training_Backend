package com.mchub.services.impl;

import com.mchub.dto.CreateBookingRequest;
import com.mchub.enums.BookingStatus;
import com.mchub.enums.NotificationType;
import com.mchub.enums.PaymentStatus;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Booking;
import com.mchub.repositories.BookingRepository;
import com.mchub.services.BookingService;
import com.mchub.services.ChatService;
import com.mchub.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ChatService chatService;
    private final NotificationService notificationService;

    private static final Set<BookingStatus> VALID_FROM_PENDING = Set.of(BookingStatus.ACCEPTED, BookingStatus.REJECTED);
    private static final Set<BookingStatus> VALID_FROM_ACCEPTED = Set.of(BookingStatus.PAID, BookingStatus.CANCELLED);
    private static final Set<BookingStatus> VALID_FROM_PAID = Set.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED);

    @Override
    public Booking createBooking(CreateBookingRequest req, String clientId) {
        boolean available = checkAvailabilityParallel(req.getMc(), req.getEventDate(), req.getStartTime(), req.getEndTime());
        if (!available) {
            throw new AppException(ErrorCode.MC_NOT_AVAILABLE);
        }

        Booking booking = Booking.builder()
                .client(clientId)
                .mc(req.getMc())
                .eventDate(req.getEventDate())
                .eventName(req.getEventName())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .location(req.getLocation())
                .eventType(req.getEventType())
                .description(req.getDescription())
                .audienceSize(req.getAudienceSize())
                .budget(req.getBudget())
                .specialRequests(req.getSpecialRequests())
                .couponCode(req.getCouponCode())
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);
        sendBookingNotifications(saved);
        initializeChatRoom(saved);
        return saved;
    }

    @Override
    public Booking updateStatus(String bookingId, BookingStatus newStatus, String mcId, Double price, String rejectionReason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getMc().equals(mcId)) {
            throw new AppException(ErrorCode.BOOKING_ACCESS_DENIED);
        }

        validateTransition(booking.getStatus(), newStatus);

        booking.setStatus(newStatus);
        booking.setDecidedAt(LocalDateTime.now());
        if (price != null && newStatus == BookingStatus.ACCEPTED) {
            booking.setPrice(price);
        }

        if (newStatus == BookingStatus.REJECTED && rejectionReason != null) {
            booking.setRejectionReason(rejectionReason);
        }

        if (newStatus == BookingStatus.REJECTED || newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.COMPLETED) {
            chatService.deactivateConversationByBookingId(bookingId);
        }

        Booking saved = bookingRepository.save(booking);

        if (newStatus == BookingStatus.ACCEPTED) {
            notificationService.notify(booking.getClient(), NotificationType.ANNOUNCEMENT,
                    "Booking accepted",
                    "Your booking \"" + booking.getEventName() + "\" has been accepted by the MC.",
                    "/m/bookings/" + bookingId, false);
        } else if (newStatus == BookingStatus.REJECTED) {
            notificationService.notify(booking.getClient(), NotificationType.ANNOUNCEMENT,
                    "Booking rejected",
                    "Your booking \"" + booking.getEventName() + "\" has been rejected.",
                    "/m/bookings/" + bookingId, false);
        }

        return saved;
    }

    @Override
    public Booking cancelBooking(String bookingId, String clientId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getClient().equals(clientId)) {
            throw new AppException(ErrorCode.BOOKING_ACCESS_DENIED);
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_COMPLETED);
        }
        if (booking.getStatus() == BookingStatus.PAID) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_PAID);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        chatService.deactivateConversationByBookingId(bookingId);
        return bookingRepository.save(booking);
    }

    @Override
    public Optional<Booking> findById(String id) {
        return bookingRepository.findById(id);
    }

    @Override
    public List<Booking> getClientBookings(String clientId) {
        return bookingRepository.findByClient(clientId);
    }

    @Override
    public List<Booking> getMCBookings(String mcId) {
        return bookingRepository.findByMc(mcId);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    @Async
    public void sendBookingNotifications(Booking booking) {
        notificationService.notify(booking.getMc(), NotificationType.ANNOUNCEMENT,
                "New booking request",
                "You received a new booking request: \"" + booking.getEventName() + "\"",
                "/m/bookings/" + booking.getId(), false);
    }

    @Override
    @Async
    public void initializeChatRoom(Booking booking) {
        chatService.createConversationForBooking(booking.getId(), booking.getClient(), booking.getMc());
    }

    private void validateTransition(BookingStatus current, BookingStatus next) {
        boolean valid = switch (current) {
            case PENDING -> VALID_FROM_PENDING.contains(next);
            case ACCEPTED -> VALID_FROM_ACCEPTED.contains(next);
            case PAID -> VALID_FROM_PAID.contains(next);
            default -> false;
        };
        if (!valid) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                    "Cannot transition from " + current + " to " + next);
        }
    }

    private boolean checkAvailabilityParallel(String mcId, java.time.LocalDate date, String start, String end) {
        CompletableFuture<Boolean> acceptedCheck = CompletableFuture.supplyAsync(() ->
                bookingRepository.findByMcAndStatus(mcId, BookingStatus.ACCEPTED)
                        .stream().noneMatch(b -> isOverlapping(b, date, start, end)));
        CompletableFuture<Boolean> paidCheck = CompletableFuture.supplyAsync(() ->
                bookingRepository.findByMcAndStatus(mcId, BookingStatus.PAID)
                        .stream().noneMatch(b -> isOverlapping(b, date, start, end)));
        return acceptedCheck.join() && paidCheck.join();
    }

    private boolean isOverlapping(Booking b, java.time.LocalDate date, String start, String end) {
        if (!b.getEventDate().equals(date)) return false;
        return start.compareTo(b.getEndTime()) < 0 && end.compareTo(b.getStartTime()) > 0;
    }
}
