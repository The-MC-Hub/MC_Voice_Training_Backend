package com.mchub.repositories;

import com.mchub.models.Review;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

  List<Review> findByMc(String mcId);

  List<Review> findByBooking(String bookingId);
}
