package com.mchub.repositories;

import com.mchub.enums.BookingStatus;
import com.mchub.models.Booking;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByClient(String clientId);

    List<Booking> findByMc(String mcId);

    long countByMc(String mcId);

    List<Booking> findByMcAndStatus(String mcId, BookingStatus status);

    List<Booking> findByStatus(BookingStatus status);

    long countByMcAndStatus(String mcId, BookingStatus status);

    long countByStatus(BookingStatus status);

    List<Booking> findByClientAndStatus(String clientId, BookingStatus status);

    @Aggregation(pipeline = {
            "{ '$match': { 'mc': ?0, 'status': 'PAID' } }",
            "{ '$group': { '_id': null, 'total': { '$sum': '$price' } } }"
    })
    Long sumPriceByMcAndStatusPaid(String mcId);
}
