package com.mchub.controllers.admin;

import com.mchub.dto.ApiResponse;
import com.mchub.models.DiscountCode;
import com.mchub.models.PlanDefinition;
import com.mchub.services.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/plans")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminPlanController {

    private final PlanService planService;

    // ── Plan endpoints ─────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanDefinition>>> getAllPlans() {
        return ResponseEntity.ok(ApiResponse.success("Plans retrieved", planService.getAllPlans()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanDefinition>> updatePlan(
            @PathVariable String id,
            @RequestBody PlanDefinition updated) {
        return ResponseEntity.ok(ApiResponse.success("Plan updated", planService.updatePlan(id, updated)));
    }

    // ── Discount code endpoints ────────────────────────────────────────

    @GetMapping("/discounts")
    public ResponseEntity<ApiResponse<List<DiscountCode>>> getAllDiscounts() {
        return ResponseEntity.ok(ApiResponse.success("Discounts retrieved", planService.getAllDiscounts()));
    }

    @PostMapping("/discounts")
    public ResponseEntity<ApiResponse<DiscountCode>> createDiscount(@RequestBody DiscountCode discount) {
        discount.setId(null); // ensure new document
        discount.setUsedCount(0);
        return ResponseEntity.ok(ApiResponse.success("Discount created", planService.saveDiscount(discount)));
    }

    @PutMapping("/discounts/{id}")
    public ResponseEntity<ApiResponse<DiscountCode>> updateDiscount(
            @PathVariable String id,
            @RequestBody DiscountCode updated) {
        return ResponseEntity.ok(ApiResponse.success("Discount updated", planService.updateDiscount(id, updated)));
    }

    @DeleteMapping("/discounts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDiscount(@PathVariable String id) {
        planService.deleteDiscount(id);
        return ResponseEntity.ok(ApiResponse.success("Discount deleted", null));
    }
}
