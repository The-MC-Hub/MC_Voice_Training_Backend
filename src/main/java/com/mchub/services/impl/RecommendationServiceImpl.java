package com.mchub.services.impl;

import com.mchub.enums.MCStatus;
import com.mchub.enums.NotificationType;
import com.mchub.models.MCProfile;
import com.mchub.models.SearchInterest;
import com.mchub.models.User;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.SearchInterestRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.NotificationService;
import com.mchub.services.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    // Minimum score for a search interest to be considered "worth notifying about"
    private static final double MATCH_THRESHOLD = 4.0;

    private final SearchInterestRepository searchInterestRepository;
    private final MCProfileRepository mcProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Async
    public void notifyMatchingClients(MCProfile profile) {
        if (profile == null || profile.getStatus() != MCStatus.AVAILABLE || profile.getUser() == null) return;

        List<SearchInterest> interests = searchInterestRepository.findAll();
        User mcUser = userRepository.findById(profile.getUser()).orElse(null);
        String mcName = mcUser != null && mcUser.getName() != null ? mcUser.getName() : "MC mới";

        for (SearchInterest interest : interests) {
            // Don't notify the same client repeatedly within 7 days for the same interest
            if (interest.getLastNotifiedAt() != null
                    && ChronoUnit.DAYS.between(interest.getLastNotifiedAt(), LocalDateTime.now()) < 7) {
                continue;
            }

            double score = scoreMatch(profile, interest);
            if (score >= MATCH_THRESHOLD) {
                sendRecommendation(interest.getClientId(), mcName, profile.getId());
                interest.setLastNotifiedAt(LocalDateTime.now());
                searchInterestRepository.save(interest);
            }
        }
    }

    @Override
    @Scheduled(cron = "0 0 */6 * * *") // every 6 hours — fallback sweep
    public void runScheduledSweep() {
        List<MCProfile> availableProfiles = mcProfileRepository.findAll().stream()
                .filter(p -> p.getStatus() == MCStatus.AVAILABLE)
                .toList();

        log.info("🔍 [Recommendation] Scheduled sweep: {} available MC profiles", availableProfiles.size());
        for (MCProfile profile : availableProfiles) {
            notifyMatchingClients(profile);
        }
    }

    private double scoreMatch(MCProfile p, SearchInterest interest) {
        double score = 0;

        if (interest.getEventTypes() != null && !interest.getEventTypes().isEmpty() && p.getEventTypes() != null) {
            long matchCount = p.getEventTypes().stream().filter(interest.getEventTypes()::contains).count();
            score += Math.min(matchCount * 1.5, 3.0);
        }

        if (interest.getRegions() != null && !interest.getRegions().isEmpty() && p.getRegions() != null) {
            boolean regionMatch = p.getRegions().stream().anyMatch(interest.getRegions()::contains);
            if (regionMatch) score += 2.0;
        }

        if (interest.getBudgetMax() != null && interest.getBudgetMax() > 0 && p.getRates() != null
                && p.getRates().getMax() > 0 && p.getRates().getMax() <= interest.getBudgetMax()) {
            score += 2.0;
        }

        if (interest.getMinExperience() != null && interest.getMinExperience() > 0
                && p.getExperience() >= interest.getMinExperience()) {
            score += 1.0;
        }

        if (p.getRating() > 0) {
            score += (p.getRating() / 5.0) * 1.0;
        }

        return score;
    }

    private void sendRecommendation(String clientId, String mcName, String mcProfileId) {
        notificationService.notify(
                Objects.requireNonNull(clientId),
                NotificationType.MC_RECOMMENDATION,
                "MC phù hợp mới xuất hiện!",
                mcName + " vừa sẵn sàng nhận sự kiện, khớp với tìm kiếm gần đây của bạn.",
                "/m/mc/" + mcProfileId,
                false
        );
    }
}
