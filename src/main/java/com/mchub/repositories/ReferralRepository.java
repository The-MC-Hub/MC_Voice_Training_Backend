package com.mchub.repositories;

import com.mchub.models.Referral;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReferralRepository extends MongoRepository<Referral, String> {

    List<Referral> findByReferrerId(String referrerId);

    List<Referral> findByReferredUserId(String referredUserId);
}
