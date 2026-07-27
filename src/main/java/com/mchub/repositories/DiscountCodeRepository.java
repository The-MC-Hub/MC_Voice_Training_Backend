package com.mchub.repositories;

import com.mchub.models.DiscountCode;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountCodeRepository extends MongoRepository<DiscountCode, String> {
  Optional<DiscountCode> findByCodeIgnoreCase(String code);
}
