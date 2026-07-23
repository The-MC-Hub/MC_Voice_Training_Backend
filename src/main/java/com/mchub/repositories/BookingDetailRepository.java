package com.mchub.repositories;

import com.mchub.models.BookingDetail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingDetailRepository extends MongoRepository<BookingDetail, String> {
    Optional<BookingDetail> findByBookingId(String bookingId);
    void deleteByBookingId(String bookingId);
}
