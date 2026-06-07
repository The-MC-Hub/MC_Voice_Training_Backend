package com.mchub.repositories;

import com.mchub.models.OtpVerification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends MongoRepository<OtpVerification, String> {
    Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email);
    void deleteAllByEmail(String email);
}
