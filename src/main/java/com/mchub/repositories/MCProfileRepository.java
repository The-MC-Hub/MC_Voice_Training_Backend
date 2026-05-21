package com.mchub.repositories;

import com.mchub.models.MCProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MCProfileRepository extends MongoRepository<MCProfile, String> {
    
    
    Optional<MCProfile> findByUser(String userId);

    
    void deleteByUser(String userId);
}
