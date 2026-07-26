package com.mchub.repositories;

import com.mchub.models.EmailLog;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository extends MongoRepository<EmailLog, String> {
  List<EmailLog> findByCampaignIdOrderBySentAtDesc(String campaignId);
}
