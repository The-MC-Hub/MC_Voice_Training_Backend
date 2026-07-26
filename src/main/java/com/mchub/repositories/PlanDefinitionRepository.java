package com.mchub.repositories;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.models.PlanDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanDefinitionRepository extends MongoRepository<PlanDefinition, String> {
  Optional<PlanDefinition> findByPlan(SubscriptionPlan plan);

  List<PlanDefinition> findByActiveTrue();
}
