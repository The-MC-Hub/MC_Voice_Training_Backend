package com.mchub.services.impl;

import com.mchub.models.Notification;
import com.mchub.repositories.NotificationRepository;
import com.mchub.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Async
    public void sendNotification(@NonNull Notification notification) {
        Notification saved = notificationRepository.save(notification);

        String destination = "/topic/notifications/" + notification.getUser();
        messagingTemplate.convertAndSend(destination, saved);
    }

    @Override
    public List<Notification> getUserNotifications(@NonNull String userId, int page, int size) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Override
    public long getUnreadCount(@NonNull String userId) {
        return notificationRepository.countByUserAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(@NonNull String id, @NonNull String userId) {
        notificationRepository.findById(Objects.requireNonNull(id)).ifPresent(n -> {
            if (n.getUser().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    public void markAllAsRead(@NonNull String userId) {
        List<Notification> unread = notificationRepository.findByUserOrderByCreatedAtDesc(userId,
                PageRequest.of(0, 500));
        unread.forEach(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    public void deleteAll(@NonNull String userId) {
        notificationRepository.deleteByUser(userId);
    }
}
