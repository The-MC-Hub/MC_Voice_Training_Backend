package com.mchub.repositories;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.models.PlanDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanDefinitionRepository extends MongoRepository<PlanDefinition, String> {
    Optional<PlanDefinition> findByPlan(SubscriptionPlan plan);
    List<PlanDefinition> findByActiveTrue();
}
