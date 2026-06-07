package com.mchub.repositories;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.models.PlanDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PlanDefinitionRepository extends MongoRepository<PlanDefinition, String> {
    Optional<PlanDefinition> findByPlan(SubscriptionPlan plan);
    List<PlanDefinition> findByActiveTrue();
}
