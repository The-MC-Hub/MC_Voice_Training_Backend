package com.mchub.controllers.admin;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.DiscountCode;
import com.mchub.models.PlanDefinition;
import com.mchub.services.PlanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminPlanController.class)
@ContextConfiguration(classes = {AdminPlanController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminPlanControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PlanService planService;

    @Nested
    @DisplayName("GET/PUT /api/v1/admin/plans")
    class PlanCrud {

        @Test
        @DisplayName("listAll delegates to getAllPlans")
        void listsAllPlans() throws Exception {
            when(planService.getAllPlans()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/plans")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("update delegates to updatePlan")
        void updatesPlan() throws Exception {
            when(planService.updatePlan(eq("plan-1"), any(PlanDefinition.class)))
                    .thenReturn(PlanDefinition.builder().id("plan-1").priceVnd(299000).build());

            mockMvc.perform(put("/api/v1/admin/plans/{id}", "plan-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"priceVnd\":299000}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/plans/seed-daily — idempotent")
    class SeedDailyPlan {

        @Test
        @DisplayName("returns existing plan without saving a new one when DAILY already exists")
        void returnsExistingWhenAlreadySeeded() throws Exception {
            PlanDefinition existing = PlanDefinition.builder().id("daily-1").plan(SubscriptionPlan.DAILY).build();
            when(planService.getPlanByKey(SubscriptionPlan.DAILY)).thenReturn(existing);

            mockMvc.perform(post("/api/v1/admin/plans/seed-daily"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Daily plan already exists"));

            verify(planService, org.mockito.Mockito.never()).savePlan(any(PlanDefinition.class));
        }

        @Test
        @DisplayName("creates a new DAILY plan when none exists yet")
        void createsNewWhenNotSeeded() throws Exception {
            when(planService.getPlanByKey(SubscriptionPlan.DAILY))
                    .thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));
            when(planService.savePlan(any(PlanDefinition.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/admin/plans/seed-daily"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.plan").value("DAILY"));

            verify(planService).savePlan(any(PlanDefinition.class));
        }
    }

    @Nested
    @DisplayName("POST /discounts, PUT /discounts/{id}")
    class DiscountCrud {

        @Test
        @DisplayName("createDiscount forces id=null and usedCount=0 regardless of client input")
        void forcesIdNullAndUsedCountZero() throws Exception {
            when(planService.saveDiscount(any(DiscountCode.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/admin/plans/discounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\":\"client-supplied\",\"code\":\"TEST10\",\"usedCount\":999}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").isEmpty())
                    .andExpect(jsonPath("$.data.usedCount").value(0));
        }

        @Test
        @DisplayName("updateDiscount delegates with the pathVariable id")
        void updatesDiscount() throws Exception {
            when(planService.updateDiscount(eq("disc-1"), any(DiscountCode.class)))
                    .thenReturn(DiscountCode.builder().id("disc-1").build());

            mockMvc.perform(put("/api/v1/admin/plans/discounts/{id}", "disc-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"NEW10\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /discounts/{id}")
    class DeleteDiscount {

        @Test
        @DisplayName("200 OK, delegates to deleteDiscount")
        void deletesDiscount() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/plans/discounts/{id}", "disc-1")).andExpect(status().isOk());

            verify(planService).deleteDiscount("disc-1");
        }
    }
}
