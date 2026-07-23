package com.mchub.repositories;

import com.mchub.models.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByMc(String mcId);

    List<Review> findByBooking(String bookingId);
}
