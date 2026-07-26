package com.mchub.services;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.models.DiscountCode;
import com.mchub.models.PlanDefinition;
import java.util.List;
import java.util.Map;

public interface PlanService {
  List<PlanDefinition> getAllPlans();

  List<PlanDefinition> getActivePlans();

  PlanDefinition getPlanByKey(SubscriptionPlan plan);

  PlanDefinition savePlan(PlanDefinition plan);

  PlanDefinition updatePlan(String id, PlanDefinition updated);

  List<DiscountCode> getAllDiscounts();

  DiscountCode saveDiscount(DiscountCode discount);

  DiscountCode updateDiscount(String id, DiscountCode updated);

  void deleteDiscount(String id);

  Map<String, Object> applyDiscount(String code, SubscriptionPlan plan);

  List<DiscountCode> getActiveFlashDeals();

  void consumeDiscount(String code);
}
