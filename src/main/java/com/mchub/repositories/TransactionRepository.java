package com.mchub.repositories;

import com.mchub.models.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    List<Transaction> findByBooking(String bookingId);

    Optional<Transaction> findByPayosOrderCode(Long payosOrderCode);

    List<Transaction> findByStatus(String status);
}
