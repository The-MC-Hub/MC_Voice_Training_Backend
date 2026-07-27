package com.mchub.repositories;

import com.mchub.models.MCProfile;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MCProfileRepository extends MongoRepository<MCProfile, String> {

  Optional<MCProfile> findByUser(String userId);

  void deleteByUser(String userId);
}
