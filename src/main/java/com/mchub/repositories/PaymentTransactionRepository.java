package com.mchub.repositories;

import com.mchub.enums.TransactionStatus;
import com.mchub.models.PaymentTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends MongoRepository<PaymentTransaction, String> {
  List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId);

  Optional<PaymentTransaction> findTopByUserIdOrderByCreatedAtDesc(String userId);

  boolean existsByMemo(String memo);

  Optional<PaymentTransaction> findByOrderCode(Long orderCode);

  List<PaymentTransaction> findAllByOrderByCreatedAtDesc();

  long countByStatus(TransactionStatus status);

  List<PaymentTransaction> findByStatus(TransactionStatus status);

  @Aggregation(
      pipeline = {
        "{ '$match': { 'status': ?0 } }",
        "{ '$group': { '_id': null, 'total': { '$sum': '$amount' } } }"
      })
  Long sumAmountByStatus(TransactionStatus status);
}
