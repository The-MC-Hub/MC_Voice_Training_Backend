package com.mchub.repositories;

import com.mchub.models.EmailCampaign;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmailCampaignRepository extends MongoRepository<EmailCampaign, String> {
    List<EmailCampaign> findAllByOrderByCreatedAtDesc();
}
