package com.mchub.repositories;

import com.mchub.models.DiscountCode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscountCodeRepository extends MongoRepository<DiscountCode, String> {
    Optional<DiscountCode> findByCodeIgnoreCase(String code);
}
