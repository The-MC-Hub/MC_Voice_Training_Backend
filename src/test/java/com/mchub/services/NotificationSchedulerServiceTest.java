package com.mchub.services;

import com.mchub.enums.NotificationType;
import com.mchub.models.UserStats;
import com.mchub.repositories.UserStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerServiceTest {

    @Mock private UserStatsRepository userStatsRepository;
    @Mock private NotificationService notificationService;

    private NotificationSchedulerService scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NotificationSchedulerService(userStatsRepository, notificationService);
    }

    @Test
    @DisplayName("notifies every at-risk user returned by the repository query")
    void notifiesAllAtRiskUsers() {
        UserStats user1 = UserStats.builder().userId("u1").currentStreak(5).lastPracticeTime(Instant.now()).build();
        UserStats user2 = UserStats.builder().userId("u2").currentStreak(12).lastPracticeTime(Instant.now()).build();
        when(userStatsRepository.findByCurrentStreakGreaterThanAndLastPracticeTimeBetween(
                eq(0), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(user1, user2));

        scheduler.sendStreakReminders();

        verify(notificationService, times(2)).notify(
                anyString(), eq(NotificationType.STREAK_REMINDER), anyString(), anyString(), anyString(), eq(false));
        verify(notificationService).notify(eq("u1"), eq(NotificationType.STREAK_REMINDER),
                anyString(), anyString(), anyString(), eq(false));
        verify(notificationService).notify(eq("u2"), eq(NotificationType.STREAK_REMINDER),
                anyString(), anyString(), anyString(), eq(false));
    }

    @Test
    @DisplayName("sends nothing when no users are at risk")
    void sendsNothingWhenNoneAtRisk() {
        when(userStatsRepository.findByCurrentStreakGreaterThanAndLastPracticeTimeBetween(
                eq(0), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        scheduler.sendStreakReminders();

        verify(notificationService, never()).notify(anyString(), any(), anyString(), anyString(), anyString(), anyBoolean());
    }
}
