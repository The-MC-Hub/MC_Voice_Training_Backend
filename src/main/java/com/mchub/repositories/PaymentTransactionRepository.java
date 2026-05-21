package com.mchub.repositories;

import com.mchub.models.PaymentTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends MongoRepository<PaymentTransaction, String> {
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<PaymentTransaction> findTopByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByMemo(String memo);
}
