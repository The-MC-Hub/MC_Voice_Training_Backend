package com.mchub.services.impl;

import com.mchub.enums.NotificationType;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Notification;
import com.mchub.models.User;
import com.mchub.repositories.NotificationRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private EmailService emailService;

    private NotificationServiceImpl service;

    private static final String USER_ID = "user-001";

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationRepository, userRepository, messagingTemplate, emailService);
    }

    @Nested
    @DisplayName("notify()")
    class Notify {

        @Test
        @DisplayName("saves notification and pushes over WebSocket")
        void savesAndPushes() throws Exception {
            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.notify(USER_ID, NotificationType.PAYMENT_SUCCESS, "Title", "Body", "/m/payment", false);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getType()).isEqualTo(NotificationType.PAYMENT_SUCCESS);
            assertThat(captor.getValue().getTitle()).isEqualTo("Title");
            assertThat(captor.getValue().isRead()).isFalse();

            verify(messagingTemplate).convertAndSend(eq("/topic/notifications/" + USER_ID), any(Object.class));
            verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("sends email when sendEmail=true and user has an email")
        void sendsEmailWhenRequested() throws Exception {
            User user = User.builder().id(USER_ID).name("Trung").email("trung@example.com").build();
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(emailService.buildHtmlEmail(anyString(), anyString(), anyString())).thenReturn("<html></html>");

            service.notify(USER_ID, NotificationType.PAYMENT_SUCCESS, "Title", "Body", "/m/payment", true);

            verify(emailService).sendHtmlEmail(eq("trung@example.com"), eq("Title"), anyString());
        }

        @Test
        @DisplayName("skips email when user has no email on file")
        void skipsEmailWhenNoAddress() {
            User user = User.builder().id(USER_ID).name("Trung").email("").build();
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            service.notify(USER_ID, NotificationType.PAYMENT_SUCCESS, "Title", "Body", "/m/payment", true);
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsRead {

        @Test
        @DisplayName("marks notification read when caller is the owner")
        void marksReadForOwner() {
            Notification n = Notification.builder().id("notif-1").userId(USER_ID).read(false).build();
            when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(n));

            service.markAsRead("notif-1", USER_ID);

            assertThat(n.isRead()).isTrue();
            verify(notificationRepository).save(n);
        }

        @Test
        @DisplayName("throws ACCESS_DENIED when caller is not the owner")
        void throwsForNonOwner() {
            Notification n = Notification.builder().id("notif-1").userId("someone-else").read(false).build();
            when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(n));

            assertThatThrownBy(() -> service.markAsRead("notif-1", USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("throws RESOURCE_NOT_FOUND for unknown id")
        void throwsForUnknownId() {
            when(notificationRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markAsRead("missing", USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("markAllAsRead()")
    class MarkAllAsRead {

        @Test
        @DisplayName("marks only unread notifications as read")
        void marksOnlyUnread() {
            Notification unread1 = Notification.builder().id("n1").userId(USER_ID).read(false).build();
            Notification unread2 = Notification.builder().id("n2").userId(USER_ID).read(false).build();
            Notification alreadyRead = Notification.builder().id("n3").userId(USER_ID).read(true).build();
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(unread1, unread2, alreadyRead));

            service.markAllAsRead(USER_ID);

            assertThat(unread1.isRead()).isTrue();
            assertThat(unread2.isRead()).isTrue();
            verify(notificationRepository).saveAll(List.of(unread1, unread2));
        }
    }

    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCount {

        @Test
        @DisplayName("delegates to repository count query")
        void delegatesToRepository() {
            when(notificationRepository.countByUserIdAndReadFalse(USER_ID)).thenReturn(3L);

            long count = service.getUnreadCount(USER_ID);

            assertThat(count).isEqualTo(3L);
        }
    }
}
