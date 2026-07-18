package com.mchub.services;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.DiscountCode;
import com.mchub.models.PlanDefinition;
import com.mchub.repositories.DiscountCodeRepository;
import com.mchub.repositories.PlanDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PlanService. Mocks PlanDefinitionRepository/DiscountCodeRepository —
 * no real MongoDB connection required. Covers the discount validation chain
 * (applyDiscount) exercised manually during UC-06 system testing, plus flash-deal
 * filtering and plan/discount CRUD.
 */
@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock private PlanDefinitionRepository planRepo;
    @Mock private DiscountCodeRepository discountRepo;

    private PlanService planService;

    @BeforeEach
    void setUp() {
        planService = new PlanService(planRepo, discountRepo);
    }

    private PlanDefinition.PlanDefinitionBuilder basicPlanDef() {
        return PlanDefinition.builder().id("plan-basic").plan(SubscriptionPlan.BASIC).priceVnd(199000).active(true);
    }

    private DiscountCode.DiscountCodeBuilder validPercentCode() {
        return DiscountCode.builder().id("disc-1").code("SUMMER30")
                .type(DiscountCode.DiscountType.PERCENT).discountValue(30)
                .maxUses(0).usedCount(0).active(true)
                .applicablePlans(List.of());
    }

    @Nested
    @DisplayName("applyDiscount")
    class ApplyDiscount {

        @Test
        @DisplayName("computes percent discount correctly")
        void computesPercentDiscount() {
            when(discountRepo.findByCodeIgnoreCase("SUMMER30")).thenReturn(Optional.of(validPercentCode().build()));
            when(planRepo.findByPlan(SubscriptionPlan.BASIC)).thenReturn(Optional.of(basicPlanDef().build()));

            Map<String, Object> result = planService.applyDiscount("SUMMER30", SubscriptionPlan.BASIC);

            assertThat(result.get("originalPrice")).isEqualTo(199000);
            assertThat(result.get("discountAmount")).isEqualTo(59700); // 30% of 199000
            assertThat(result.get("finalPrice")).isEqualTo(139300);
        }

        @Test
        @DisplayName("computes fixed discount correctly, capped at original price")
        void computesFixedDiscountCapped() {
            DiscountCode fixedCode = DiscountCode.builder().id("disc-2").code("BIG500K")
                    .type(DiscountCode.DiscountType.FIXED).discountValue(500000)
                    .maxUses(0).usedCount(0).active(true).applicablePlans(List.of()).build();
            when(discountRepo.findByCodeIgnoreCase("BIG500K")).thenReturn(Optional.of(fixedCode));
            when(planRepo.findByPlan(SubscriptionPlan.BASIC)).thenReturn(Optional.of(basicPlanDef().build()));

            Map<String, Object> result = planService.applyDiscount("BIG500K", SubscriptionPlan.BASIC);

            // discount capped at originalPrice (199000), not the full 500000
            assertThat(result.get("discountAmount")).isEqualTo(199000);
            assertThat(result.get("finalPrice")).isEqualTo(0);
        }

        @Test
        @DisplayName("throws RESOURCE_NOT_FOUND for an unknown code")
        void throwsWhenCodeNotFound() {
            when(discountRepo.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.applyDiscount("NOPE", SubscriptionPlan.BASIC))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED for an inactive code")
        void throwsWhenInactive() {
            DiscountCode inactive = validPercentCode().active(false).build();
            when(discountRepo.findByCodeIgnoreCase("SUMMER30")).thenReturn(Optional.of(inactive));

            assertThatThrownBy(() -> planService.applyDiscount("SUMMER30", SubscriptionPlan.BASIC))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED for an expired code")
        void throwsWhenExpired() {
            DiscountCode expired = validPercentCode().expiresAt(LocalDateTime.now().minusDays(1)).build();
            when(discountRepo.findByCodeIgnoreCase("SUMMER30")).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> planService.applyDiscount("SUMMER30", SubscriptionPlan.BASIC))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED when maxUses reached")
        void throwsWhenExhausted() {
            DiscountCode exhausted = validPercentCode().maxUses(10).usedCount(10).build();
            when(discountRepo.findByCodeIgnoreCase("SUMMER30")).thenReturn(Optional.of(exhausted));

            assertThatThrownBy(() -> planService.applyDiscount("SUMMER30", SubscriptionPlan.BASIC))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED when code does not apply to the requested plan")
        void throwsWhenPlanNotApplicable() {
            DiscountCode restricted = validPercentCode().applicablePlans(List.of(SubscriptionPlan.FULL)).build();
            when(discountRepo.findByCodeIgnoreCase("SUMMER30")).thenReturn(Optional.of(restricted));

            assertThatThrownBy(() -> planService.applyDiscount("SUMMER30", SubscriptionPlan.BASIC))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("allows code when applicablePlans is empty (applies to all plans)")
        void allowsWhenApplicablePlansEmpty() {
            when(discountRepo.findByCodeIgnoreCase("SUMMER30")).thenReturn(Optional.of(validPercentCode().build()));
            when(planRepo.findByPlan(SubscriptionPlan.BASIC)).thenReturn(Optional.of(basicPlanDef().build()));

            Map<String, Object> result = planService.applyDiscount("SUMMER30", SubscriptionPlan.BASIC);

            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("does NOT increment usedCount — that only happens in consumeDiscount")
        void doesNotIncrementUsedCount() {
            when(discountRepo.findByCodeIgnoreCase("SUMMER30")).thenReturn(Optional.of(validPercentCode().build()));
            when(planRepo.findByPlan(SubscriptionPlan.BASIC)).thenReturn(Optional.of(basicPlanDef().build()));

            planService.applyDiscount("SUMMER30", SubscriptionPlan.BASIC);

            verify(discountRepo, never()).save(any(DiscountCode.class));
        }
    }

    @Nested
    @DisplayName("consumeDiscount")
    class ConsumeDiscount {

        @Test
        @DisplayName("increments usedCount and saves")
        void incrementsUsedCount() {
            DiscountCode code = validPercentCode().usedCount(3).build();
            when(discountRepo.findByCodeIgnoreCase("SUMMER30")).thenReturn(Optional.of(code));

            planService.consumeDiscount("SUMMER30");

            assertThat(code.getUsedCount()).isEqualTo(4);
            verify(discountRepo).save(code);
        }

        @Test
        @DisplayName("silently no-ops when code not found")
        void noOpsWhenCodeMissing() {
            when(discountRepo.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

            planService.consumeDiscount("NOPE");

            verify(discountRepo, never()).save(any(DiscountCode.class));
        }
    }

    @Nested
    @DisplayName("getActiveFlashDeals")
    class GetActiveFlashDeals {

        @Test
        @DisplayName("filters out codes not marked showInSidebar")
        void filtersOutNonSidebarCodes() {
            DiscountCode notInSidebar = validPercentCode().showInSidebar(false).build();
            when(discountRepo.findAll()).thenReturn(List.of(notInSidebar));

            List<DiscountCode> result = planService.getActiveFlashDeals();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("includes codes within their active time window")
        void includesCodesWithinWindow() {
            DiscountCode inWindow = validPercentCode().showInSidebar(true)
                    .startsAt(LocalDateTime.now().minusDays(1))
                    .expiresAt(LocalDateTime.now().plusDays(1)).build();
            when(discountRepo.findAll()).thenReturn(List.of(inWindow));

            List<DiscountCode> result = planService.getActiveFlashDeals();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("excludes codes that haven't started yet")
        void excludesNotYetStarted() {
            DiscountCode notStarted = validPercentCode().showInSidebar(true)
                    .startsAt(LocalDateTime.now().plusDays(1)).build();
            when(discountRepo.findAll()).thenReturn(List.of(notStarted));

            List<DiscountCode> result = planService.getActiveFlashDeals();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("excludes exhausted codes (usedCount >= maxUses)")
        void excludesExhaustedCodes() {
            DiscountCode exhausted = validPercentCode().showInSidebar(true).maxUses(5).usedCount(5).build();
            when(discountRepo.findAll()).thenReturn(List.of(exhausted));

            List<DiscountCode> result = planService.getActiveFlashDeals();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("excludes inactive codes even if within window")
        void excludesInactiveCodes() {
            DiscountCode inactive = validPercentCode().showInSidebar(true).active(false).build();
            when(discountRepo.findAll()).thenReturn(List.of(inactive));

            List<DiscountCode> result = planService.getActiveFlashDeals();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("plan CRUD")
    class PlanCrud {

        @Test
        @DisplayName("getPlanByKey throws RESOURCE_NOT_FOUND for unknown plan")
        void throwsWhenPlanMissing() {
            when(planRepo.findByPlan(SubscriptionPlan.ANNUAL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.getPlanByKey(SubscriptionPlan.ANNUAL))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("getActivePlans returns only active=true plans")
        void returnsOnlyActivePlans() {
            when(planRepo.findByActiveTrue()).thenReturn(List.of(basicPlanDef().build()));

            List<PlanDefinition> result = planService.getActivePlans();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("updatePlan overwrites all mutable fields and preserves id")
        void updatePlanOverwritesFields() {
            PlanDefinition existing = basicPlanDef().priceVnd(199000).displayName("Old Name").build();
            when(planRepo.findById("plan-basic")).thenReturn(Optional.of(existing));
            when(planRepo.save(any(PlanDefinition.class))).thenAnswer(inv -> inv.getArgument(0));

            PlanDefinition updated = basicPlanDef().priceVnd(249000).displayName("New Name").build();

            PlanDefinition result = planService.updatePlan("plan-basic", updated);

            assertThat(result.getId()).isEqualTo("plan-basic");
            assertThat(result.getPriceVnd()).isEqualTo(249000);
            assertThat(result.getDisplayName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("updatePlan throws RESOURCE_NOT_FOUND for unknown id")
        void updatePlanThrowsWhenMissing() {
            when(planRepo.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.updatePlan("missing", basicPlanDef().build()))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("discount CRUD")
    class DiscountCrud {

        @Test
        @DisplayName("saveDiscount normalizes code to uppercase")
        void normalizesCodeToUppercase() {
            DiscountCode lower = DiscountCode.builder().id("d1").code("summer30")
                    .type(DiscountCode.DiscountType.PERCENT).discountValue(10).build();
            when(discountRepo.findByCodeIgnoreCase("SUMMER30")).thenReturn(Optional.empty());
            when(discountRepo.save(any(DiscountCode.class))).thenAnswer(inv -> inv.getArgument(0));

            DiscountCode result = planService.saveDiscount(lower);

            assertThat(result.getCode()).isEqualTo("SUMMER30");
        }

        @Test
        @DisplayName("saveDiscount throws VALIDATION_FAILED when code already used by a different discount")
        void throwsOnDuplicateCode() {
            DiscountCode newCode = DiscountCode.builder().id("new-id").code("SUMMER30")
                    .type(DiscountCode.DiscountType.PERCENT).discountValue(10).build();
            DiscountCode existingCode = DiscountCode.builder().id("existing-id").code("SUMMER30").build();
            when(discountRepo.findByCodeIgnoreCase("SUMMER30")).thenReturn(Optional.of(existingCode));

            assertThatThrownBy(() -> planService.saveDiscount(newCode))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("saveDiscount allows re-saving the SAME discount (id matches)")
        void allowsResavingSameDiscount() {
            DiscountCode existing = DiscountCode.builder().id("same-id").code("SUMMER30")
                    .type(DiscountCode.DiscountType.PERCENT).discountValue(10).build();
            when(discountRepo.findByCodeIgnoreCase("SUMMER30")).thenReturn(Optional.of(existing));
            when(discountRepo.save(any(DiscountCode.class))).thenAnswer(inv -> inv.getArgument(0));

            DiscountCode result = planService.saveDiscount(existing);

            assertThat(result.getCode()).isEqualTo("SUMMER30");
        }

        @Test
        @DisplayName("deleteDiscount delegates to repository deleteById")
        void deletesDiscount() {
            planService.deleteDiscount("disc-1");

            verify(discountRepo).deleteById("disc-1");
        }
    }
}
