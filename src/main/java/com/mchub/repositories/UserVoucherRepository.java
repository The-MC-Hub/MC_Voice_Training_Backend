package com.mchub.repositories;

import com.mchub.models.UserVoucher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserVoucherRepository extends MongoRepository<UserVoucher, String> {
  List<UserVoucher> findByUserIdOrderByCreatedAtDesc(String userId);

  List<UserVoucher> findByUserIdAndUsedAtIsNullAndActiveTrue(String userId);

  @Query(
      "{ 'userId': ?0, 'usedAt': null, 'active': true, '$or': [ { 'expiresAt': null }, { 'expiresAt': { '$gt': ?1 } } ] }")
  List<UserVoucher> findAvailableVouchers(String userId, LocalDateTime now);

  Optional<UserVoucher> findByUserIdAndCode(String userId, String code);

  boolean existsByUserIdAndCode(String userId, String code);
}
