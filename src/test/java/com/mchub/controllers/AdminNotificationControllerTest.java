package com.mchub.controllers;

import com.mchub.enums.NotificationType;
import com.mchub.enums.UserRole;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.mapper.NotificationMapper;
import com.mchub.models.Notification;
import com.mchub.models.User;
import com.mchub.repositories.NotificationRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.AuditLogService;
import com.mchub.services.NotificationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminNotificationController.class)
@ContextConfiguration(classes = {AdminNotificationController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminNotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private NotificationRepository notificationRepository;
    @MockBean private NotificationMapper notificationMapper;
    @MockBean private NotificationService notificationService;
    @MockBean private UserRepository userRepository;
    @MockBean private AuditLogService auditLogService;

    private static final String ADMIN_ID = "admin-001";

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
    @DisplayName("GET /api/v1/admin/notifications")
    class GetAllNotifications {

        @Test
        @DisplayName("200 OK with mapped list")
        void returnsAllNotifications() throws Exception {
            when(notificationRepository.findAll()).thenReturn(List.of(
                    Notification.builder().id("n1").userId("u1").build()));

            mockMvc.perform(get("/api/v1/admin/notifications")).andExpect(status().isOk());

            verify(notificationRepository).findAll();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/notifications/stats")
    class GetStats {

        @Test
        @DisplayName("200 OK with counts")
        void returnsStats() throws Exception {
            when(notificationRepository.count()).thenReturn(10L);
            when(notificationRepository.countByRead(false)).thenReturn(3L);
            when(notificationRepository.countByType(any())).thenReturn(1L);

            mockMvc.perform(get("/api/v1/admin/notifications/stats")).andExpect(status().isOk());

            verify(notificationRepository).count();
            verify(notificationRepository).countByRead(false);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/notifications/send")
    class SendNotification {

        @Test
        @DisplayName("targetType=USER sends to exactly one user")
        void sendsToSingleUser() throws Exception {
            mockMvc.perform(post("/api/v1/admin/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"USER\",\"userId\":\"u1\",\"type\":\"ANNOUNCEMENT\",\"title\":\"Hi\",\"body\":\"Body\"}"))
                    .andExpect(status().isOk());

            verify(notificationService, times(1)).notify(
                    eq("u1"), eq(NotificationType.ANNOUNCEMENT), eq("Hi"), eq("Body"), any(), eq(false));
        }

        @Test
        @DisplayName("targetType=ROLE resolves via UserRepository.findByRole and sends to each")
        void sendsToRole() throws Exception {
            when(userRepository.findByRole(UserRole.MC)).thenReturn(List.of(
                    User.builder().id("mc1").build(), User.builder().id("mc2").build()));

            mockMvc.perform(post("/api/v1/admin/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"ROLE\",\"role\":\"MC\",\"type\":\"ANNOUNCEMENT\",\"title\":\"Hi\",\"body\":\"Body\"}"))
                    .andExpect(status().isOk());

            verify(notificationService, times(2)).notify(
                    anyString(), eq(NotificationType.ANNOUNCEMENT), anyString(), anyString(), any(), eq(false));
        }

        @Test
        @DisplayName("targetType=ALL excludes admins via findByRoleNot")
        void sendsToAllExcludingAdmins() throws Exception {
            when(userRepository.findByRoleNot(UserRole.ADMIN)).thenReturn(List.of(
                    User.builder().id("u1").build(), User.builder().id("u2").build(), User.builder().id("u3").build()));

            mockMvc.perform(post("/api/v1/admin/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"ALL\",\"type\":\"ANNOUNCEMENT\",\"title\":\"Hi\",\"body\":\"Body\"}"))
                    .andExpect(status().isOk());

            verify(userRepository).findByRoleNot(UserRole.ADMIN);
            verify(notificationService, times(3)).notify(
                    anyString(), eq(NotificationType.ANNOUNCEMENT), anyString(), anyString(), any(), eq(false));
        }

        @Test
        @DisplayName("400 when userId missing for targetType=USER")
        void rejectsMissingUserId() throws Exception {
            mockMvc.perform(post("/api/v1/admin/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"USER\",\"type\":\"ANNOUNCEMENT\",\"title\":\"Hi\",\"body\":\"Body\"}"))
                    .andExpect(status().isBadRequest());

            verify(notificationService, never()).notify(anyString(), any(), anyString(), anyString(), any(), eq(false));
        }

        @Test
        @DisplayName("400 when title is blank — bean validation")
        void rejectsBlankTitle() throws Exception {
            mockMvc.perform(post("/api/v1/admin/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"ALL\",\"type\":\"ANNOUNCEMENT\",\"title\":\"\",\"body\":\"Body\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/notifications/{id}")
    class DeleteNotification {

        @Test
        @DisplayName("200 OK deletes existing notification")
        void deletesExisting() throws Exception {
            when(notificationRepository.existsById("n1")).thenReturn(true);

            mockMvc.perform(delete("/api/v1/admin/notifications/{id}", "n1")).andExpect(status().isOk());

            verify(notificationRepository).deleteById("n1");
        }

        @Test
        @DisplayName("404 when notification does not exist")
        void returns404ForMissing() throws Exception {
            when(notificationRepository.existsById("missing")).thenReturn(false);

            mockMvc.perform(delete("/api/v1/admin/notifications/{id}", "missing")).andExpect(status().isNotFound());

            verify(notificationRepository, never()).deleteById(anyString());
        }
    }
}
