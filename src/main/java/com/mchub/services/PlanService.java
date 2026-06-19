package com.mchub.services;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.DiscountCode;
import com.mchub.models.PlanDefinition;
import com.mchub.repositories.DiscountCodeRepository;
import com.mchub.repositories.PlanDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanDefinitionRepository planRepo;
    private final DiscountCodeRepository discountRepo;

    // ── Plan CRUD ──────────────────────────────────────────────────────

    public List<PlanDefinition> getAllPlans() {
        return planRepo.findAll();
    }

    public List<PlanDefinition> getActivePlans() {
        return planRepo.findByActiveTrue();
    }

    public PlanDefinition getPlanByKey(SubscriptionPlan plan) {
        return planRepo.findByPlan(plan)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Plan not found: " + plan));
    }

    public PlanDefinition savePlan(PlanDefinition plan) {
        return planRepo.save(plan);
    }

    public PlanDefinition updatePlan(String id, PlanDefinition updated) {
        PlanDefinition existing = planRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Plan not found: " + id));
        existing.setPriceVnd(updated.getPriceVnd());
        existing.setDurationDays(updated.getDurationDays());
        existing.setAiSessionLimit(updated.getAiSessionLimit());
        existing.setDisplayName(updated.getDisplayName());
        existing.setTagline(updated.getTagline());
        existing.setDescription(updated.getDescription());
        existing.setBadge(updated.getBadge());
        existing.setUrgencyText(updated.getUrgencyText());
        existing.setSocialProof(updated.getSocialProof());
        existing.setHighlights(updated.getHighlights());
        existing.setComparisonEntries(updated.getComparisonEntries());
        existing.setDiscountedPriceVnd(updated.getDiscountedPriceVnd());
        existing.setDiscountPercent(updated.getDiscountPercent());
        existing.setActive(updated.isActive());
        return planRepo.save(existing);
    }

    // ── Discount CRUD ──────────────────────────────────────────────────

    public List<DiscountCode> getAllDiscounts() {
        return discountRepo.findAll();
    }

    public DiscountCode saveDiscount(DiscountCode discount) {
        // Normalize code to uppercase
        discount.setCode(discount.getCode().toUpperCase().trim());
        // Check duplicate
        discountRepo.findByCodeIgnoreCase(discount.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(discount.getId())) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Code already exists: " + discount.getCode());
            }
        });
        return discountRepo.save(discount);
    }

    public DiscountCode updateDiscount(String id, DiscountCode updated) {
        DiscountCode existing = discountRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Discount not found: " + id));
        existing.setCode(updated.getCode().toUpperCase().trim());
        existing.setType(updated.getType());
        existing.setDiscountValue(updated.getDiscountValue());
        existing.setApplicablePlans(updated.getApplicablePlans());
        existing.setMaxUses(updated.getMaxUses());
        existing.setStartsAt(updated.getStartsAt());
        existing.setExpiresAt(updated.getExpiresAt());
        existing.setActive(updated.isActive());
        existing.setDescription(updated.getDescription());
        return discountRepo.save(existing);
    }

    public void deleteDiscount(String id) {
        discountRepo.deleteById(id);
    }

    // ── Apply discount ─────────────────────────────────────────────────

    /**
     * Validates a discount code for a given plan, returns final price and discount info.
     * Does NOT increment usedCount — that happens when payment is confirmed.
     */
    public Map<String, Object> applyDiscount(String code, SubscriptionPlan plan) {
        DiscountCode dc = discountRepo.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Mã giảm giá không tồn tại"));

        if (!dc.isActive()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã giảm giá đã hết hiệu lực");
        }
        if (dc.getExpiresAt() != null && dc.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã giảm giá đã hết hạn");
        }
        if (dc.getMaxUses() > 0 && dc.getUsedCount() >= dc.getMaxUses()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã giảm giá đã hết lượt sử dụng");
        }
        if (dc.getApplicablePlans() != null && !dc.getApplicablePlans().isEmpty()
                && !dc.getApplicablePlans().contains(plan)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã giảm giá không áp dụng cho gói " + plan.name());
        }

        PlanDefinition planDef = planRepo.findByPlan(plan)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Plan not found"));

        int originalPrice = planDef.getPriceVnd();
        int discount;
        if (dc.getType() == DiscountCode.DiscountType.PERCENT) {
            discount = originalPrice * dc.getDiscountValue() / 100;
        } else {
            discount = Math.min(dc.getDiscountValue(), originalPrice);
        }
        int finalPrice = Math.max(0, originalPrice - discount);

        return Map.of(
                "code", dc.getCode(),
                "type", dc.getType().name(),
                "discountValue", dc.getDiscountValue(),
                "discountAmount", discount,
                "originalPrice", originalPrice,
                "finalPrice", finalPrice,
                "description", dc.getDescription() != null ? dc.getDescription() : ""
        );
    }

    /**
     * Returns all discount codes that are currently within their active time window,
     * have remaining uses, and are marked active. Used by the flash-deal sidebar.
     */
    public List<DiscountCode> getActiveFlashDeals() {
        LocalDateTime now = LocalDateTime.now();
        return discountRepo.findAll().stream()
                .filter(dc -> dc.isActive())
                .filter(dc -> dc.getStartsAt() == null || !dc.getStartsAt().isAfter(now))
                .filter(dc -> dc.getExpiresAt() == null || dc.getExpiresAt().isAfter(now))
                .filter(dc -> dc.getMaxUses() <= 0 || dc.getUsedCount() < dc.getMaxUses())
                .toList();
    }

    /** Called after payment confirmed — increment usedCount */
    public void consumeDiscount(String code) {
        discountRepo.findByCodeIgnoreCase(code).ifPresent(dc -> {
            dc.setUsedCount(dc.getUsedCount() + 1);
            discountRepo.save(dc);
        });
    }
}
