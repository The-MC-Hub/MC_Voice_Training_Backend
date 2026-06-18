package com.mchub.repositories;

import com.mchub.models.GuestVoiceUsage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuestVoiceUsageRepository extends MongoRepository<GuestVoiceUsage, String> {
}
