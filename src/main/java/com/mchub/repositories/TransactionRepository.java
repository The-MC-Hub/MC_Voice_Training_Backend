package com.mchub.repositories;

import com.mchub.models.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

  List<Transaction> findByBooking(String bookingId);

  Optional<Transaction> findByPayosOrderCode(Long payosOrderCode);

  List<Transaction> findByStatus(String status);
}
