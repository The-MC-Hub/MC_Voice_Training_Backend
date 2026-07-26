package com.mchub.repositories;

import com.mchub.models.ClientProfile;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClientProfileRepository extends MongoRepository<ClientProfile, String> {
  Optional<ClientProfile> findByUser(String userId);
}
