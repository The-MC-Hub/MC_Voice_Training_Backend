package com.mchub.services.impl;

import com.mchub.dto.MCProfileResponseDTO;
import com.mchub.dto.MCTrainingStatsDTO;
import com.mchub.enums.UserRole;
import com.mchub.mapper.MCProfileMapper;
import com.mchub.models.MCProfile;
import com.mchub.models.PracticeSession;
import com.mchub.models.User;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PublicServiceImpl. Regression guard for the audit fix
 * (Remaining_Modules_Audit_Report.md 2.3): getFeaturedMCTrainingStats() used
 * to call practiceSessionRepository.findByUserId() inside a per-profile loop —
 * now batch-fetched once via findByUserIdIn() before the loop.
 */
@ExtendWith(MockitoExtension.class)
class PublicServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private MCProfileRepository mcProfileRepository;
    @Mock private MCProfileMapper mcProfileMapper;
    @Mock private PracticeSessionRepository practiceSessionRepository;

    private PublicServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublicServiceImpl(userRepository, mcProfileRepository, mcProfileMapper, practiceSessionRepository);
    }

    @Nested
    @DisplayName("getFeaturedMCTrainingStats — regression guard for N+1 DB-call-in-loop fix")
    class GetFeaturedMCTrainingStats {

        @Test
        @DisplayName("batch-fetches sessions for ALL MC profiles in a single call — never per-profile inside the loop")
        void batchFetchesSessionsOnce() {
            MCProfile p1 = MCProfile.builder().id("mc-1").user("user-1").build();
            MCProfile p2 = MCProfile.builder().id("mc-2").user("user-2").build();
            when(mcProfileRepository.findAll()).thenReturn(List.of(p1, p2));
            when(userRepository.findAllById(List.of("user-1", "user-2")))
                    .thenReturn(List.of(User.builder().id("user-1").name("MC One").build(),
                            User.builder().id("user-2").name("MC Two").build()));
            when(practiceSessionRepository.findByUserIdIn(List.of("user-1", "user-2"))).thenReturn(List.of());

            service.getFeaturedMCTrainingStats();

            // exactly ONE batch call, never per-profile calls
            verify(practiceSessionRepository, org.mockito.Mockito.times(1)).findByUserIdIn(any());
            verify(practiceSessionRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("computes avgAccuracy/avgRhythm correctly for a profile's grouped sessions")
        void computesAveragesPerProfile() {
            MCProfile p1 = MCProfile.builder().id("mc-1").user("user-1").build();
            when(mcProfileRepository.findAll()).thenReturn(List.of(p1));
            when(userRepository.findAllById(List.of("user-1")))
                    .thenReturn(List.of(User.builder().id("user-1").name("MC One").avatar("avatar.png").build()));
            List<PracticeSession> sessions = List.of(
                    PracticeSession.builder().userId("user-1").accuracyScore(80.0).rhythmScore(70.0).build(),
                    PracticeSession.builder().userId("user-1").accuracyScore(90.0).rhythmScore(90.0).build());
            when(practiceSessionRepository.findByUserIdIn(List.of("user-1"))).thenReturn(sessions);

            List<MCTrainingStatsDTO> result = service.getFeaturedMCTrainingStats();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAvgAccuracy()).isEqualTo(85.0);
            assertThat(result.get(0).getAvgRhythm()).isEqualTo(80.0);
            assertThat(result.get(0).getTotalSessions()).isEqualTo(2);
        }

        @Test
        @DisplayName("defaults name to 'Elite MC' when the user record is missing")
        void defaultsNameWhenUserMissing() {
            MCProfile p1 = MCProfile.builder().id("mc-1").user("user-missing").build();
            when(mcProfileRepository.findAll()).thenReturn(List.of(p1));
            when(userRepository.findAllById(List.of("user-missing"))).thenReturn(List.of());
            when(practiceSessionRepository.findByUserIdIn(List.of("user-missing"))).thenReturn(List.of());

            List<MCTrainingStatsDTO> result = service.getFeaturedMCTrainingStats();

            assertThat(result.get(0).getName()).isEqualTo("Elite MC");
        }

        @Test
        @DisplayName("skips profiles with a null user reference")
        void skipsProfilesWithNullUser() {
            MCProfile orphan = MCProfile.builder().id("mc-orphan").user(null).build();
            when(mcProfileRepository.findAll()).thenReturn(List.of(orphan));
            when(userRepository.findAllById(List.of())).thenReturn(List.of());
            when(practiceSessionRepository.findByUserIdIn(List.of())).thenReturn(List.of());

            List<MCTrainingStatsDTO> result = service.getFeaturedMCTrainingStats();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns zeroed averages for a profile with no practice sessions")
        void zeroedAveragesWhenNoSessions() {
            MCProfile p1 = MCProfile.builder().id("mc-1").user("user-1").build();
            when(mcProfileRepository.findAll()).thenReturn(List.of(p1));
            when(userRepository.findAllById(List.of("user-1")))
                    .thenReturn(List.of(User.builder().id("user-1").name("MC One").build()));
            when(practiceSessionRepository.findByUserIdIn(List.of("user-1"))).thenReturn(List.of());

            List<MCTrainingStatsDTO> result = service.getFeaturedMCTrainingStats();

            assertThat(result.get(0).getAvgAccuracy()).isZero();
            assertThat(result.get(0).getAvgRhythm()).isZero();
            assertThat(result.get(0).getTotalSessions()).isZero();
        }
    }

    @Nested
    @DisplayName("getLandingData")
    class GetLandingData {

        @Test
        @DisplayName("aggregates MC count and MC-role user count concurrently")
        void aggregatesStatsConcurrently() {
            when(mcProfileRepository.count()).thenReturn(42L);
            when(userRepository.countByRole(UserRole.MC)).thenReturn(30L);

            Map<String, Object> result = service.getLandingData();

            @SuppressWarnings("unchecked")
            Map<String, Object> stats = (Map<String, Object>) result.get("stats");
            assertThat(stats.get("totalMCs")).isEqualTo(42L);
            assertThat(stats.get("totalProfessionals")).isEqualTo(30L);
        }
    }

    @Nested
    @DisplayName("discoverMCs")
    class DiscoverMCs {

        @Test
        @DisplayName("maps each profile with its corresponding user via batch-fetched map")
        void mapsProfilesWithUsers() {
            MCProfile p1 = MCProfile.builder().id("mc-1").user("user-1").build();
            when(mcProfileRepository.findAll()).thenReturn(List.of(p1));
            User user1 = User.builder().id("user-1").name("MC One").build();
            when(userRepository.findAllById(List.of("user-1"))).thenReturn(List.of(user1));
            MCProfileResponseDTO dto = new MCProfileResponseDTO();
            when(mcProfileMapper.toResponseDTO(p1, user1)).thenReturn(dto);

            List<MCProfileResponseDTO> result = service.discoverMCs(null);

            assertThat(result).containsExactly(dto);
        }
    }

    @Nested
    @DisplayName("getMCProfile")
    class GetMCProfile {

        @Test
        @DisplayName("looks up by user field first, falls back to profile id if not found")
        void fallsBackToProfileId() {
            when(mcProfileRepository.findByUser("some-id")).thenReturn(java.util.Optional.empty());
            MCProfile profile = MCProfile.builder().id("some-id").user("user-1").build();
            when(mcProfileRepository.findById("some-id")).thenReturn(java.util.Optional.of(profile));
            User user = User.builder().id("user-1").build();
            when(userRepository.findById("user-1")).thenReturn(java.util.Optional.of(user));
            MCProfileResponseDTO dto = new MCProfileResponseDTO();
            when(mcProfileMapper.toResponseDTO(profile, user)).thenReturn(dto);

            MCProfileResponseDTO result = service.getMCProfile("some-id");

            assertThat(result).isSameAs(dto);
        }

        @Test
        @DisplayName("returns null when neither lookup finds a profile")
        void returnsNullWhenNotFound() {
            when(mcProfileRepository.findByUser("missing")).thenReturn(java.util.Optional.empty());
            when(mcProfileRepository.findById("missing")).thenReturn(java.util.Optional.empty());

            MCProfileResponseDTO result = service.getMCProfile("missing");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getUserRoles / getReportReasons — enum-to-DTO conversion")
    class EnumOptions {

        @Test
        @DisplayName("getUserRoles returns all UserRole enum values, sorted by label")
        void returnsAllUserRoles() {
            var result = service.getUserRoles();

            assertThat(result).hasSize(UserRole.values().length);
            assertThat(result).isSortedAccordingTo((a, b) -> a.getLabel().compareTo(b.getLabel()));
        }

        @Test
        @DisplayName("formats enum name as Title Case with spaces (e.g. ADMIN -> Admin)")
        void formatsLabelAsTitleCase() {
            var result = service.getUserRoles();

            var adminOption = result.stream().filter(o -> o.getValue().equals("ADMIN")).findFirst().orElseThrow();
            assertThat(adminOption.getLabel()).isEqualTo("Admin");
        }
    }
}
