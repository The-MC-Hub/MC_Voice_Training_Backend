package com.mchub.services;

import com.mchub.enums.NotificationType;
import com.mchub.models.Notification;
import org.springframework.lang.NonNull;

import java.util.List;

public interface NotificationService {

    /**
     * Fire-and-forget entry point. Saves the notification, pushes it over WebSocket to
     * /topic/notifications/{userId}, and optionally emails the user. Callers never block on
     * DB write, WS push, or email send (implementation is @Async).
     */
    void notify(@NonNull String userId, @NonNull NotificationType type, @NonNull String title,
                @NonNull String body, String actionUrl, boolean sendEmail);

    List<Notification> getMyNotifications(@NonNull String userId);

    long getUnreadCount(@NonNull String userId);

    void markAsRead(@NonNull String notificationId, @NonNull String userId);

    void markAllAsRead(@NonNull String userId);
}
