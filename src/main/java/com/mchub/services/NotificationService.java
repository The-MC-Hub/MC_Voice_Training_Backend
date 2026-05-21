package com.mchub.services;

import com.mchub.models.Notification;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface NotificationService {

        void sendNotification(@NonNull Notification notification);

    @PreAuthorize("hasAuthority('ADMIN') or #userId == authentication.name")
    List<Notification> getUserNotifications(@NonNull String userId, int page, int size);

    @PreAuthorize("hasAuthority('ADMIN') or #userId == authentication.name")
    long getUnreadCount(@NonNull String userId);

    @PreAuthorize("#userId == authentication.name")
    void markAsRead(@NonNull String id, @NonNull String userId);

    @PreAuthorize("#userId == authentication.name")
    void markAllAsRead(@NonNull String userId);

    @PreAuthorize("#userId == authentication.name")
    void deleteAll(@NonNull String userId);
}
