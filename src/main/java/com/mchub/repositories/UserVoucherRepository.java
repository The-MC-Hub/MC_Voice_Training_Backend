package com.mchub.repositories;

import com.mchub.models.UserVoucher;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends MongoRepository<UserVoucher, String> {
    List<UserVoucher> findByUserIdOrderByCreatedAtDesc(String userId);
    List<UserVoucher> findByUserIdAndUsedAtIsNullAndActiveTrue(String userId);
    Optional<UserVoucher> findByUserIdAndCode(String userId, String code);
    boolean existsByUserIdAndCode(String userId, String code);
}
