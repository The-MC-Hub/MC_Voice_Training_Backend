package com.mchub.services.impl;

import com.mchub.models.MCProfile;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.services.MCProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class MCProfileServiceImpl implements MCProfileService {

    private final MCProfileRepository mcProfileRepository;
    private final PracticeSessionRepository practiceSessionRepository;

    @Override
    public Map<String, Object> getDashboardStats(String userId) {
        CompletableFuture<Long> totalPracticesFuture = CompletableFuture.supplyAsync(
                () -> practiceSessionRepository.countByUserId(userId));

        CompletableFuture<Double> avgAccuracyFuture = CompletableFuture.supplyAsync(
                () -> {
                    // This would ideally be a custom aggregation in repository
                    var sessions = practiceSessionRepository.findByUserId(userId);
                    return sessions.stream().mapToDouble(s -> s.getAccuracyScore()).average().orElse(0.0);
                });

        CompletableFuture<Double> avgWpmFuture = CompletableFuture.supplyAsync(
                () -> {
                    var sessions = practiceSessionRepository.findByUserId(userId);
                    return sessions.stream().mapToDouble(s -> s.getSpeakingRateWpm()).average().orElse(0.0);
                });

        CompletableFuture.allOf(totalPracticesFuture, avgAccuracyFuture, avgWpmFuture).join();

        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalPractices", totalPracticesFuture.get());
            stats.put("avgAccuracy", avgAccuracyFuture.get());
            stats.put("avgWpm", avgWpmFuture.get());
        } catch (Exception e) {
            throw new RuntimeException("Error aggregating statistics: " + e.getMessage());
        }

        return stats;
    }

    @Override
    public MCProfile updateProfile(String userId, MCProfile profileData) {
        MCProfile existing = mcProfileRepository.findByUser(userId)
                .orElse(new MCProfile());

        existing.setUser(userId);
        if (profileData.getExperience() > 0) {
            existing.setExperience(profileData.getExperience());
        }
        if (profileData.getBiography() != null) {
            existing.setBiography(profileData.getBiography());
        }
        if (profileData.getPersonality() != null) {
            existing.setPersonality(profileData.getPersonality());
        }
        if (profileData.getHostingStyle() != null) {
            existing.setHostingStyle(profileData.getHostingStyle());
        }
        if (profileData.getLanguages() != null && !profileData.getLanguages().isEmpty()) {
            existing.setLanguages(profileData.getLanguages());
        }
        if (profileData.getStyles() != null && !profileData.getStyles().isEmpty()) {
            existing.setStyles(profileData.getStyles());
        }

        return mcProfileRepository.save(existing);
    }

}
