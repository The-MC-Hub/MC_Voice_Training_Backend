package com.mchub.repositories;

import com.mchub.models.Referral;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferralRepository extends MongoRepository<Referral, String> {

  List<Referral> findByReferrerId(String referrerId);

  List<Referral> findByReferredUserId(String referredUserId);
}
