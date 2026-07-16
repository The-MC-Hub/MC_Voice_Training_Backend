package com.mchub.services.impl;

import com.mchub.dto.EnumOptionDTO;
import com.mchub.dto.MCProfileResponseDTO;
import com.mchub.dto.MCTrainingStatsDTO;
import com.mchub.enums.*;
import com.mchub.mapper.MCProfileMapper;
import com.mchub.models.MCProfile;
import com.mchub.models.User;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.PublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PublicServiceImpl implements PublicService {

    private final UserRepository userRepository;
    private final MCProfileRepository mcProfileRepository;
    private final MCProfileMapper mcProfileMapper;
    private final PracticeSessionRepository practiceSessionRepository;

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
