package com.mchub.controllers;

import com.mchub.dto.UserResponseDTO;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.SystemSetting;
import com.mchub.repositories.SystemSettingRepository;
import com.mchub.services.AdminService;
import com.mchub.services.AuditLogService;
import com.mchub.services.impl.DatabaseMigrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest for AdminController. Class-level @PreAuthorize("hasAuthority('ADMIN')")
 * is not enforced in this slice (method security requires the full app
 * context) — role-boundary testing is covered by SecurityConfig manual review
 * and UC-09 system testing. Here we exercise request validation, delegation
 * to AdminService, and audit logging side effects. migrateDb is deliberately
 * NOT exercised end-to-end per DEFECT-003 (hardcoded production DB name) —
 * only the controller wiring (that it calls migrationService.migrateFromMcHub())
 * is verified via a mock, never a real call.
 */
@WebMvcTest(controllers = AdminController.class)
@ContextConfiguration(classes = {AdminController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminService adminService;
    @MockBean private DatabaseMigrationService migrationService;
    @MockBean private AuditLogService auditLogService;
    @MockBean private SystemSettingRepository systemSettingRepo;

    private static final String ADMIN_ID = "admin-001";
    private static final String TARGET_ID = "user-target-001";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ADMIN_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET dashboard/transactions/revenue-stats/analytics — pure delegation")
    class ReadOnlyDelegation {

        @Test
        @DisplayName("dashboard delegates to adminService.getAdminDashboardOverview")
        void dashboardDelegates() throws Exception {
            when(adminService.getAdminDashboardOverview()).thenReturn(Map.of("totalUsers", 100));

            mockMvc.perform(get("/api/v1/admin/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalUsers").value(100));
        }

        @Test
        @DisplayName("users/mcs delegates to adminService.getAllMCs")
        void usersMcsDelegates() throws Exception {
            when(adminService.getAllMCs()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/users/mcs"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{id}/status")
    class UpdateUserStatus {

        @Test
        @DisplayName("400 VALIDATION_FAILED when isActive or isVerified is missing from body")
        void rejectsMissingFields() throws Exception {
            mockMvc.perform(put("/api/v1/admin/users/{id}/status", TARGET_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isActive\":true}"))
                    .andExpect(status().isBadRequest());

            verify(adminService, never()).updateUserStatus(anyString(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyBoolean());
        }

        @Test
        @DisplayName("200 OK and logs ADMIN_UPDATE_USER_STATUS on success")
        void updatesAndLogsAudit() throws Exception {
            when(adminService.updateUserStatus(TARGET_ID, false, true)).thenReturn(new UserResponseDTO());

            mockMvc.perform(put("/api/v1/admin/users/{id}/status", TARGET_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isActive\":false,\"isVerified\":true}"))
                    .andExpect(status().isOk());

            verify(auditLogService).log(org.mockito.ArgumentMatchers.eq(ADMIN_ID), any(), anyString(), org.mockito.ArgumentMatchers.eq(TARGET_ID), anyString(), any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{id}/plan")
    class UpdateUserPlan {

        @Test
        @DisplayName("400 VALIDATION_FAILED when plan is blank")
        void rejectsBlankPlan() throws Exception {
            mockMvc.perform(put("/api/v1/admin/users/{id}/plan", TARGET_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"plan\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("propagates VALIDATION_FAILED from service for an unparseable plan string")
        void propagatesServiceValidationError() throws Exception {
            when(adminService.updateUserPlan(TARGET_ID, "GARBAGE"))
                    .thenThrow(new AppException(ErrorCode.VALIDATION_FAILED, "Invalid plan"));

            mockMvc.perform(put("/api/v1/admin/users/{id}/plan", TARGET_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"plan\":\"GARBAGE\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/users — createUser")
    class CreateUser {

        @Test
        @DisplayName("400 VALIDATION_FAILED when name/email/password missing")
        void rejectsMissingRequiredFields() throws Exception {
            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("defaults role to CLIENT when not supplied")
        void defaultsRoleToClient() throws Exception {
            UserResponseDTO dto = new UserResponseDTO();
            dto.setId("new-user-id");
            when(adminService.createUser(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.eq("CLIENT"),
                    any(), any(), any(), any())).thenReturn(dto);

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"email\":\"t@test.local\",\"password\":\"pass1234\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value("new-user-id"));
        }

        @Test
        @DisplayName("409 EMAIL_ALREADY_EXISTS propagated from service")
        void propagatesEmailAlreadyExists() throws Exception {
            when(adminService.createUser(anyString(), anyString(), anyString(), anyString(),
                    any(), any(), any(), any())).thenThrow(new AppException(ErrorCode.EMAIL_ALREADY_EXISTS));

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"email\":\"taken@test.local\",\"password\":\"pass1234\"}"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/users/{id}/change-password")
    class ChangePassword {

        @Test
        @DisplayName("400 VALIDATION_FAILED when newPassword is blank")
        void rejectsBlankPassword() throws Exception {
            mockMvc.perform(post("/api/v1/admin/users/{id}/change-password", TARGET_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\":\"\"}"))
                    .andExpect(status().isBadRequest());

            verify(adminService, never()).changeUserPassword(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/users/{id} — soft delete + audit")
    class DeleteUser {

        @Test
        @DisplayName("200 OK, delegates to adminService.deleteUser, logs ADMIN_DELETE_USER")
        void deletesAndLogsAudit() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/users/{id}", TARGET_ID))
                    .andExpect(status().isOk());

            verify(adminService).deleteUser(TARGET_ID);
            verify(auditLogService).log(org.mockito.ArgumentMatchers.eq(ADMIN_ID), any(), anyString(), org.mockito.ArgumentMatchers.eq(TARGET_ID), any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/users/{id}/notify-email")
    class NotifyEmail {

        @Test
        @DisplayName("400 VALIDATION_FAILED when subject or content is blank")
        void rejectsBlankSubjectOrContent() throws Exception {
            mockMvc.perform(post("/api/v1/admin/users/{id}/notify-email", TARGET_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"subject\":\"\",\"content\":\"Body\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/migrate-db — wiring only, never exercised for real (DEFECT-003)")
    class MigrateDb {

        @Test
        @DisplayName("controller calls migrationService.migrateFromMcHub() exactly once and logs audit")
        void callsMigrationServiceOnce() throws Exception {
            mockMvc.perform(post("/api/v1/admin/migrate-db"))
                    .andExpect(status().isOk());

            verify(migrationService).migrateFromMcHub();
            verify(auditLogService).log(org.mockito.ArgumentMatchers.eq(ADMIN_ID), any(), anyString(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("GET/PUT guest-cooldown settings")
    class GuestCooldownSettings {

        @Test
        @DisplayName("GET defaults to 3 hours when no setting exists")
        void defaultsToThreeHoursWhenUnset() throws Exception {
            when(systemSettingRepo.findById("GUEST_COOLDOWN_HOURS")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/admin/settings/guest-cooldown"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hours").value(3));
        }

        @Test
        @DisplayName("GET returns stored value when present")
        void returnsStoredValue() throws Exception {
            SystemSetting setting = new SystemSetting();
            setting.setKey("GUEST_COOLDOWN_HOURS");
            setting.setValue("12");
            when(systemSettingRepo.findById("GUEST_COOLDOWN_HOURS")).thenReturn(Optional.of(setting));

            mockMvc.perform(get("/api/v1/admin/settings/guest-cooldown"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hours").value(12));
        }

        @Test
        @DisplayName("GET falls back to 3 when stored value is unparseable")
        void fallsBackWhenUnparseable() throws Exception {
            SystemSetting setting = new SystemSetting();
            setting.setKey("GUEST_COOLDOWN_HOURS");
            setting.setValue("not-a-number");
            when(systemSettingRepo.findById("GUEST_COOLDOWN_HOURS")).thenReturn(Optional.of(setting));

            mockMvc.perform(get("/api/v1/admin/settings/guest-cooldown"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hours").value(3));
        }

        @Test
        @DisplayName("PUT rejects hours < 1")
        void rejectsHoursBelowMin() throws Exception {
            mockMvc.perform(put("/api/v1/admin/settings/guest-cooldown").param("hours", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT rejects hours > 168")
        void rejectsHoursAboveMax() throws Exception {
            mockMvc.perform(put("/api/v1/admin/settings/guest-cooldown").param("hours", "200"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT accepts boundary value 168 and persists it")
        void acceptsUpperBoundary() throws Exception {
            when(systemSettingRepo.findById("GUEST_COOLDOWN_HOURS")).thenReturn(Optional.empty());
            when(systemSettingRepo.save(any(SystemSetting.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(put("/api/v1/admin/settings/guest-cooldown").param("hours", "168"))
                    .andExpect(status().isOk());

            verify(systemSettingRepo).save(org.mockito.ArgumentMatchers.argThat(s -> "168".equals(s.getValue())));
        }
    }
}
