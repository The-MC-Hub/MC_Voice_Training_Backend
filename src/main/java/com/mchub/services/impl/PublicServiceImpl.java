package com.mchub.services.impl;

import com.mchub.dto.EnumOptionDTO;
import com.mchub.dto.MCSearchResultDTO;
import com.mchub.dto.MCProfileResponseDTO;
import com.mchub.dto.MCTrainingStatsDTO;
import com.mchub.dto.SearchMCRequest;
import com.mchub.enums.*;
import com.mchub.mapper.MCProfileMapper;
import com.mchub.models.MCProfile;
import com.mchub.models.SearchInterest;
import com.mchub.models.User;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.SearchInterestRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.PublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PublicServiceImpl implements PublicService {

    private final UserRepository userRepository;
    private final MCProfileRepository mcProfileRepository;
    private final MCProfileMapper mcProfileMapper;
    private final PracticeSessionRepository practiceSessionRepository;
    private final SearchInterestRepository searchInterestRepository;

    @Override
    public List<MCTrainingStatsDTO> getFeaturedMCTrainingStats() {
        List<MCProfile> profiles = mcProfileRepository.findAll(); // In real app, filter by featured
        
        List<String> userIds = profiles.stream()
                .map(MCProfile::getUser)
                .filter(Objects::nonNull)
                .toList();

        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        // Batch-fetch all sessions for all MC users in one query — no DB calls inside loop
        Map<String, List<com.mchub.models.PracticeSession>> sessionsByUser = practiceSessionRepository
                .findByUserIdIn(userIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(com.mchub.models.PracticeSession::getUserId));

        List<MCTrainingStatsDTO> statsList = new ArrayList<>();

        for (MCProfile profile : profiles) {
            String uId = profile.getUser();
            if (uId == null) continue;

            User user = userMap.get(uId);
            List<com.mchub.models.PracticeSession> sessions = sessionsByUser.getOrDefault(uId, List.of());

            double totalHours = sessions.size() * 0.5; // Mock: each session is 30 mins
            double avgAcc = sessions.stream().mapToDouble(com.mchub.models.PracticeSession::getAccuracyScore).average().orElse(0.0);
            double avgRhy = sessions.stream().mapToDouble(com.mchub.models.PracticeSession::getRhythmScore).average().orElse(0.0);

            statsList.add(MCTrainingStatsDTO.builder()
                    .mcId(profile.getId())
                    .userId(uId)
                    .name(user != null ? user.getName() : "Elite MC")
                    .avatar(user != null ? user.getAvatar() : null)
                    .totalPracticeHours(totalHours)
                    .scriptsCompleted(sessions.size())
                    .avgAccuracy(avgAcc)
                    .avgRhythm(avgRhy)
                    .totalSessions(sessions.size())
                    .build());
        }

        return statsList;
    }

    @Override
    public Map<String, Object> getLandingData() {
        CompletableFuture<Long> mcsCount = CompletableFuture.supplyAsync(mcProfileRepository::count);
        CompletableFuture<Long> professionalsCount = CompletableFuture
                .supplyAsync(() -> userRepository.countByRole(UserRole.MC));

        CompletableFuture.allOf(mcsCount, professionalsCount).join();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMCs", mcsCount.join());
        stats.put("totalProfessionals", professionalsCount.join());

        return Map.of("stats", stats);
    }

    @Override
    public List<MCProfileResponseDTO> discoverMCs(String category) {
        List<MCProfile> profiles = mcProfileRepository.findAll();

        if (category != null && !category.isBlank()) {
            profiles = profiles.stream()
                    .filter(p -> p.getStyles() != null && p.getStyles().contains(category))
                    .toList();
        }

        List<String> userIds = profiles.stream()
                .map(MCProfile::getUser)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        return profiles.stream()
                .map(profile -> mcProfileMapper.toResponseDTO(profile, userMap.get(profile.getUser())))
                .toList();
    }

    @Override
    public MCProfileResponseDTO getMCProfile(String id) {
        MCProfile profile = mcProfileRepository.findByUser(id).orElse(null);

        if (profile == null) {
            profile = mcProfileRepository.findById(Objects.requireNonNull(id)).orElse(null);
        }

        if (profile == null)
            return null;

        User user = userRepository.findById(Objects.requireNonNull(profile.getUser())).orElse(null);
        return mcProfileMapper.toResponseDTO(profile, user);
    }


    @Override
    public List<MCSearchResultDTO> searchMCs(SearchMCRequest req) {
        List<MCProfile> profiles = mcProfileRepository.findAll();
        List<String> userIds = profiles.stream().map(MCProfile::getUser).filter(Objects::nonNull).distinct().toList();
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        // Save search interest if client is logged in
        try {
            String currentUserId = com.mchub.util.SecurityUtils.getCurrentUserId();
            if (currentUserId != null) {
                saveSearchInterest(currentUserId, req);
            }
        } catch (Exception ignored) {
            // not logged in — skip interest tracking
        }

        return profiles.stream()
                .filter(p -> p.getStatus() == MCStatus.AVAILABLE)
                .map(p -> {
                    Map<String, Object> breakdown = new java.util.LinkedHashMap<>();
                    double score = 0;

                    // 1. Keyword match — also checks languages, notableEvents, personality, hostingStyle
                    if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                        String kw = req.getKeyword().toLowerCase();
                        User u = userMap.get(p.getUser());
                        boolean nameMatch = u != null && u.getName() != null && u.getName().toLowerCase().contains(kw);
                        boolean styleMatch = p.getStyles() != null && p.getStyles().stream().anyMatch(s -> s.toLowerCase().contains(kw));
                        boolean bioMatch = p.getBiography() != null && p.getBiography().toLowerCase().contains(kw);
                        boolean langMatch = p.getLanguages() != null && p.getLanguages().stream().anyMatch(l -> l.toLowerCase().contains(kw));
                        boolean notableMatch = p.getNotableEvents() != null && p.getNotableEvents().stream().anyMatch(n -> n.toLowerCase().contains(kw));
                        boolean personalityMatch = (p.getPersonality() != null && p.getPersonality().toLowerCase().contains(kw))
                                || (p.getHostingStyle() != null && p.getHostingStyle().toLowerCase().contains(kw));
                        if (nameMatch || styleMatch || bioMatch || langMatch || notableMatch || personalityMatch) {
                            double kScore = 2.0;
                            if (nameMatch) kScore += 0.3;
                            if (bioMatch) kScore += 0.2;
                            score += kScore;
                            breakdown.put("keyword_match", kScore);
                        } else {
                            breakdown.put("keyword_match", 0);
                        }
                    }

                    // 2. Event type match — with specificity bonus
                    if (req.getEventTypes() != null && !req.getEventTypes().isEmpty()) {
                        long matchCount = p.getEventTypes().stream()
                                .filter(et -> req.getEventTypes().contains(et))
                                .count();
                        double specificity = (double) matchCount / Math.max(req.getEventTypes().size(), 1);
                        double etScore = matchCount * 1.5 + specificity * 1.0;
                        etScore = Math.min(etScore, 3.5);
                        if (etScore > 0) {
                            score += etScore;
                            breakdown.put("event_type_match", Math.round(etScore * 100.0) / 100.0);
                        } else {
                            breakdown.put("event_type_match", 0);
                        }
                    }

                    // 3. Region match — proportional scoring
                    if (req.getRegions() != null && !req.getRegions().isEmpty()) {
                        if (p.getRegions() != null && !p.getRegions().isEmpty()) {
                            long regionMatchCount = p.getRegions().stream().filter(r -> req.getRegions().contains(r)).count();
                            double regionScore = Math.min(regionMatchCount * 1.5, 2.5);
                            score += regionScore;
                            breakdown.put("region_match", regionScore);
                        } else {
                            breakdown.put("region_match", 0);
                        }
                    }

                    // 4. Budget fit — bidirectional + tight-fit bonus
                    if (p.getRates() != null) {
                        double budgetScore = 0;
                        if (req.getBudgetMax() != null && req.getBudgetMax() > 0 && p.getRates().getMax() <= req.getBudgetMax()) {
                            budgetScore += 1.5;
                        }
                        if (req.getBudgetMin() != null && req.getBudgetMin() > 0 && p.getRates().getMin() >= req.getBudgetMin()) {
                            budgetScore += 1.0;
                        }
                        if (budgetScore >= 2.5 && req.getBudgetMax() != null && req.getBudgetMax() > 0 && p.getRates().getMax() > 0) {
                            double tightFit = 1.0 - (p.getRates().getMax() / req.getBudgetMax());
                            if (tightFit > 0) budgetScore += Math.min(tightFit * 0.5, 0.5);
                        }
                        if (budgetScore > 0) {
                            score += budgetScore;
                            breakdown.put("budget_fit", Math.round(budgetScore * 100.0) / 100.0);
                        } else {
                            breakdown.put("budget_fit", 0);
                        }
                    }

                    // 5. Experience bonus — scaled up to 1.5
                    if (req.getMinExperience() != null && req.getMinExperience() > 0 && p.getExperience() >= req.getMinExperience()) {
                        double expBonus = Math.min(p.getExperience() / 10.0, 1.5);
                        score += expBonus;
                        breakdown.put("experience_bonus", expBonus);
                    }

                    // 6. Rating bonus
                    if (p.getRating() > 0) {
                        double ratingScore = (p.getRating() / 5.0) * 1.5;
                        score += ratingScore;
                        breakdown.put("rating_bonus", Math.round(ratingScore * 100.0) / 100.0);
                    }

                    // 7. Total events hosted — proven track record
                    if (p.getTotalEvents() >= 10) {
                        double eventsBonus = Math.min(p.getTotalEvents() / 200.0, 1.5);
                        score += eventsBonus;
                        breakdown.put("events_bonus", Math.round(eventsBonus * 100.0) / 100.0);
                    }

                    // 8. Response time bonus — fast response = premium
                    if (p.getResponseTime() > 0 && p.getResponseTime() <= 60) {
                        double responseBonus = (60.0 - p.getResponseTime()) / 60.0 * 1.0;
                        score += responseBonus;
                        breakdown.put("response_bonus", Math.round(responseBonus * 100.0) / 100.0);
                    }

                    // 9. Language diversity bonus
                    if (p.getLanguages() != null && p.getLanguages().size() > 1) {
                        double langBonus = Math.min((p.getLanguages().size() - 1) * 0.25, 0.5);
                        score += langBonus;
                        breakdown.put("language_bonus", langBonus);
                    }

                    // 10. Recency bonus — recently active MCs
                    if (p.getLastActive() != null) {
                        long daysSinceActive = java.time.Duration.between(p.getLastActive(), LocalDateTime.now()).toDays();
                        if (daysSinceActive <= 30) {
                            double recencyBonus = (30.0 - daysSinceActive) / 30.0 * 0.5;
                            score += recencyBonus;
                            breakdown.put("recency_bonus", Math.round(recencyBonus * 100.0) / 100.0);
                        }
                    }

                    breakdown.put("total", Math.round(score * 100.0) / 100.0);

                    return MCSearchResultDTO.builder()
                            .profile(mcProfileMapper.toResponseDTO(p, userMap.get(p.getUser())))
                            .score(Math.round(score * 100.0) / 100.0)
                            .scoreBreakdown(breakdown)
                            .build();
                })
                .sorted((a, b) -> {
                    if ("rating".equals(req.getSortBy())) return Double.compare(b.getProfile().getRating(), a.getProfile().getRating());
                    if ("experience".equals(req.getSortBy())) return Integer.compare(b.getProfile().getExperience(), a.getProfile().getExperience());
                    if ("price_low".equals(req.getSortBy())) return Double.compare(
                            a.getProfile().getRatesMin() != null ? a.getProfile().getRatesMin() : 0,
                            b.getProfile().getRatesMin() != null ? b.getProfile().getRatesMin() : 0);
                    if ("price_high".equals(req.getSortBy())) return Double.compare(
                            b.getProfile().getRatesMax() != null ? b.getProfile().getRatesMax() : 0,
                            a.getProfile().getRatesMax() != null ? a.getProfile().getRatesMax() : 0);
                    return Double.compare(b.getScore(), a.getScore());
                })
                .toList();
    }

    private void saveSearchInterest(String clientId, SearchMCRequest req) {
        try {
            String key = req.getKeyword() != null ? req.getKeyword().trim().toLowerCase() : "search";
            SearchInterest interest = searchInterestRepository.findByClientIdAndKeyword(clientId, key)
                    .orElse(SearchInterest.builder()
                            .clientId(clientId)
                            .keyword(key)
                            .eventTypes(req.getEventTypes())
                            .regions(req.getRegions())
                            .budgetMin(req.getBudgetMin())
                            .budgetMax(req.getBudgetMax())
                            .minExperience(req.getMinExperience())
                            .firstSearchedAt(LocalDateTime.now())
                            .searchCount(0)
                            .build());
            interest.setSearchCount(interest.getSearchCount() + 1);
            interest.setLastSearchedAt(LocalDateTime.now());
            if (req.getEventTypes() != null) interest.setEventTypes(req.getEventTypes());
            if (req.getRegions() != null) interest.setRegions(req.getRegions());
            searchInterestRepository.save(interest);
        } catch (Exception ignored) {
            // Don't let interest tracking fail the search
        }
    }

    @Override public List<EnumOptionDTO> getUserRoles() { return toEnumOptions(UserRole.class); }

    @Override public List<EnumOptionDTO> getReportReasons() { return toEnumOptions(ReportReason.class); }

        private <E extends Enum<E>> List<EnumOptionDTO> toEnumOptions(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(e -> new EnumOptionDTO(e.name(), formatLabel(e.name())))
                .sorted(Comparator.comparing(EnumOptionDTO::getLabel))
                .toList();
    }

        private String formatLabel(String name) {
        if (name == null || name.isEmpty()) return "";
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
