package com.mchub.repositories;

import com.mchub.models.EmailCampaign;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface EmailCampaignRepository extends MongoRepository<EmailCampaign, String> {
    List<EmailCampaign> findAllByOrderByCreatedAtDesc();
}
