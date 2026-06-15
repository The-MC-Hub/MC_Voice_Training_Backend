package com.mchub.repositories;

import com.mchub.models.Referral;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReferralRepository extends MongoRepository<Referral, String> {

    List<Referral> findByReferrerId(String referrerId);

    List<Referral> findByReferredUserId(String referredUserId);
}
