package com.mchub.repositories;

import com.mchub.models.EmailLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmailLogRepository extends MongoRepository<EmailLog, String> {
    List<EmailLog> findByCampaignIdOrderBySentAtDesc(String campaignId);
}
