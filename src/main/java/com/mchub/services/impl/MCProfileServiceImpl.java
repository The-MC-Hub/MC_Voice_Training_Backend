package com.mchub.services.impl;

import com.mchub.models.MCProfile;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.services.MCProfileService;
import com.mchub.services.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class MCProfileServiceImpl implements MCProfileService {

    private final MCProfileRepository mcProfileRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final RecommendationService recommendationService;

    @Override
    public Map<String, Object> getDashboardStats(String userId) {
        CompletableFuture<Long> totalPracticesFuture = CompletableFuture.supplyAsync(
                () -> practiceSessionRepository.countByUserId(userId));

        // Single fetch reused for both averages — avoids a duplicate DB round-trip
        CompletableFuture<Map<String, Double>> averagesFuture = CompletableFuture.supplyAsync(
                () -> {
                    var sessions = practiceSessionRepository.findByUserId(userId);
                    double avgAccuracy = sessions.stream().mapToDouble(s -> s.getAccuracyScore()).average().orElse(0.0);
                    double avgWpm = sessions.stream().mapToDouble(s -> s.getSpeakingRateWpm()).average().orElse(0.0);
                    Map<String, Double> averages = new HashMap<>();
                    averages.put("avgAccuracy", avgAccuracy);
                    averages.put("avgWpm", avgWpm);
                    return averages;
                });

        CompletableFuture.allOf(totalPracticesFuture, averagesFuture).join();

        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalPractices", totalPracticesFuture.get());
            Map<String, Double> averages = averagesFuture.get();
            stats.put("avgAccuracy", averages.get("avgAccuracy"));
            stats.put("avgWpm", averages.get("avgWpm"));
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
        if (profileData.getPersonality() != null && !profileData.getPersonality().isBlank()) {
            existing.setPersonality(profileData.getPersonality());
        }
        if (profileData.getHostingStyle() != null && !profileData.getHostingStyle().isBlank()) {
            existing.setHostingStyle(profileData.getHostingStyle());
        }
        if (profileData.getLanguages() != null && !profileData.getLanguages().isEmpty()) {
            existing.setLanguages(profileData.getLanguages());
        }
        if (profileData.getStyles() != null && !profileData.getStyles().isEmpty()) {
            existing.setStyles(profileData.getStyles());
        }
        if (profileData.getRegions() != null && !profileData.getRegions().isEmpty()) {
            existing.setRegions(profileData.getRegions());
        }
        if (profileData.getEventTypes() != null && !profileData.getEventTypes().isEmpty()) {
            existing.setEventTypes(profileData.getEventTypes());
        }
        if (profileData.getRates() != null) {
            existing.setRates(profileData.getRates());
        }
        if (profileData.getStatus() != null) {
            existing.setStatus(profileData.getStatus());
        }
        if (profileData.getNotableEvents() != null && !profileData.getNotableEvents().isEmpty()) {
            existing.setNotableEvents(profileData.getNotableEvents());
        }
        if (profileData.getSocialLinks() != null) {
            existing.setSocialLinks(profileData.getSocialLinks());
        }
        if (profileData.getPortfolio() != null && !profileData.getPortfolio().isEmpty()) {
            existing.setPortfolio(profileData.getPortfolio());
        }
        if (profileData.getResponseTime() > 0) {
            existing.setResponseTime(profileData.getResponseTime());
        }
        if (profileData.getTotalEvents() > 0) {
            existing.setTotalEvents(profileData.getTotalEvents());
        }
        if (profileData.getAchievements() != null && !profileData.getAchievements().isEmpty()) {
            existing.setAchievements(profileData.getAchievements());
        }
        if (profileData.getPreferredContact() != null && !profileData.getPreferredContact().isBlank()) {
            existing.setPreferredContact(profileData.getPreferredContact());
        }
        if (profileData.getVisibleFields() != null) {
            existing.setVisibleFields(profileData.getVisibleFields());
        }
        if (profileData.getEvents() != null) {
            existing.setEvents(profileData.getEvents());
        }
        existing.setLastActive(LocalDateTime.now());

        MCProfile saved = mcProfileRepository.save(existing);
        recommendationService.notifyMatchingClients(saved);
        return saved;
    }

    @Override
    public MCProfile getOwnProfile(String userId) {
        return mcProfileRepository.findByUser(userId).orElse(new MCProfile());
    }

}
