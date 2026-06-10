package com.mchub.repositories;

import com.mchub.models.EmailLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface EmailLogRepository extends MongoRepository<EmailLog, String> {
    List<EmailLog> findByCampaignIdOrderBySentAtDesc(String campaignId);
}
