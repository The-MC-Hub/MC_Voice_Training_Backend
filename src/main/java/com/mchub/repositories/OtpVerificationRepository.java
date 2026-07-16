package com.mchub.repositories;

import com.mchub.models.OtpVerification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends MongoRepository<OtpVerification, String> {
    Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email);
    void deleteAllByEmail(String email);
}
