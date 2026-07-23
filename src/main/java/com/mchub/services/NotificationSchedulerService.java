package com.mchub.services;

import com.mchub.enums.NotificationType;
import com.mchub.models.UserStats;
import com.mchub.repositories.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSchedulerService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserStatsRepository userStatsRepository;
    private final NotificationService notificationService;

    // 12:00 UTC == 19:00 ICT (server assumed UTC, e.g. Render). If the JVM's default zone is
    // already Asia/Ho_Chi_Minh, change this to "0 0 19 * * *".
    @Scheduled(cron = "0 0 12 * * *")
    public void sendStreakReminders() {
        Instant todayStart = LocalDate.now(VN_ZONE).atStartOfDay(VN_ZONE).toInstant();
        Instant yesterdayStart = todayStart.minus(1, ChronoUnit.DAYS);

        List<UserStats> atRisk = userStatsRepository
                .findByCurrentStreakGreaterThanAndLastPracticeTimeBetween(0, yesterdayStart, todayStart);

        for (UserStats stats : atRisk) {
            notificationService.notify(stats.getUserId(), NotificationType.STREAK_REMINDER,
                    "Đừng để mất chuỗi luyện tập!",
                    "Bạn đang giữ chuỗi " + stats.getCurrentStreak() + " ngày. Luyện tập hôm nay để không bị mất streak.",
                    "/m/voice/library", false);
        }
        log.info("🔔 Streak reminders sent to {} users", atRisk.size());
    }
}
