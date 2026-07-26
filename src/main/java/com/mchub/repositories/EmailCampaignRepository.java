package com.mchub.repositories;

import com.mchub.models.EmailCampaign;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailCampaignRepository extends MongoRepository<EmailCampaign, String> {
  List<EmailCampaign> findAllByOrderByCreatedAtDesc();
}
