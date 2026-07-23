package com.mchub.services.impl;

import com.mchub.enums.NotificationType;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Notification;
import com.mchub.repositories.NotificationRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.EmailService;
import com.mchub.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    @Override
    @Async
    public void notify(@NonNull String userId, @NonNull NotificationType type, @NonNull String title,
                        @NonNull String body, String actionUrl, boolean sendEmail) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .read(false)
                .build();
        notification = notificationRepository.save(Objects.requireNonNull(notification));

        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, toPayload(notification));
        } catch (Exception e) {
            log.warn("⚠️ [Notification] WS push failed for user {}: {}", userId, e.getMessage());
        }

        if (sendEmail) {
            userRepository.findById(userId).ifPresent(user -> {
                if (user.getEmail() == null || user.getEmail().isBlank()) return;
                try {
                    String recipientName = user.getName() != null ? user.getName() : "bạn";
                    String html = emailService.buildHtmlEmail(recipientName, body, "NOTIFICATION");
                    emailService.sendHtmlEmail(user.getEmail(), title, html);
                } catch (Exception e) {
                    log.warn("⚠️ [Notification] Email failed for user {}: {}", userId, e.getMessage());
                }
            });
        }
    }

    @Override
    public List<Notification> getMyNotifications(@NonNull String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public long getUnreadCount(@NonNull String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public void markAsRead(@NonNull String notificationId, @NonNull String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Notification not found: " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You do not have permission to access this notification");
        }
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    public void markAllAsRead(@NonNull String userId) {
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().filter(n -> !n.isRead()).toList();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private Map<String, Object> toPayload(Notification n) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", n.getId());
        payload.put("type", n.getType());
        payload.put("title", n.getTitle());
        payload.put("body", n.getBody());
        payload.put("actionUrl", n.getActionUrl());
        payload.put("read", n.isRead());
        payload.put("createdAt", n.getCreatedAt());
        return payload;
    }
}
