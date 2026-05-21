package com.mchub.repositories;

import com.mchub.models.Certificate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends MongoRepository<Certificate, String> {
    List<Certificate> findByUserId(String userId);
    Optional<Certificate> findByUserIdAndCourseId(String userId, String courseId);
    boolean existsByUserIdAndCourseId(String userId, String courseId);
}
