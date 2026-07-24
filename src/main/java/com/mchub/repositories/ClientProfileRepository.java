package com.mchub.repositories;

import com.mchub.models.ClientProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ClientProfileRepository extends MongoRepository<ClientProfile, String> {
    Optional<ClientProfile> findByUser(String userId);
}
