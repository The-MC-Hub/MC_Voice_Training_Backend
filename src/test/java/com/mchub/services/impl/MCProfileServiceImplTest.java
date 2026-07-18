package com.mchub.services.impl;

import com.mchub.models.MCProfile;
import com.mchub.models.PracticeSession;
import com.mchub.repositories.MCProfileRepository;
import com.mchub.repositories.PracticeSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MCProfileServiceImpl. Verifies the DEFECT fix from the
 * audit report (section 2.4): getDashboardStats() used to call
 * practiceSessionRepository.findByUserId() twice (once per CompletableFuture)
 * for the same data — now a single fetch is reused for both averages.
 */
@ExtendWith(MockitoExtension.class)
class MCProfileServiceImplTest {

    @Mock private MCProfileRepository mcProfileRepository;
    @Mock private PracticeSessionRepository practiceSessionRepository;

    private MCProfileServiceImpl service;

    private static final String USER_ID = "user-mc-001";

    @BeforeEach
    void setUp() {
        service = new MCProfileServiceImpl(mcProfileRepository, practiceSessionRepository);
    }

    @Nested
    @DisplayName("getDashboardStats")
    class GetDashboardStats {

        @Test
        @DisplayName("computes totalPractices, avgAccuracy, avgWpm from sessions")
        void computesStatsFromSessions() {
            List<PracticeSession> sessions = List.of(
                    PracticeSession.builder().id("s1").userId(USER_ID).accuracyScore(80.0).speakingRateWpm(140.0).build(),
                    PracticeSession.builder().id("s2").userId(USER_ID).accuracyScore(90.0).speakingRateWpm(160.0).build());
            when(practiceSessionRepository.countByUserId(USER_ID)).thenReturn(2L);
            when(practiceSessionRepository.findByUserId(USER_ID)).thenReturn(sessions);

            Map<String, Object> result = service.getDashboardStats(USER_ID);

            assertThat(result.get("totalPractices")).isEqualTo(2L);
            assertThat((Double) result.get("avgAccuracy")).isEqualTo(85.0);
            assertThat((Double) result.get("avgWpm")).isEqualTo(150.0);
        }

        @Test
        @DisplayName("only calls findByUserId ONCE — regression guard for the duplicate-fetch defect fixed in audit")
        void fetchesSessionsOnlyOnce() {
            when(practiceSessionRepository.countByUserId(USER_ID)).thenReturn(0L);
            when(practiceSessionRepository.findByUserId(USER_ID)).thenReturn(List.of());

            service.getDashboardStats(USER_ID);

            org.mockito.Mockito.verify(practiceSessionRepository, org.mockito.Mockito.times(1)).findByUserId(USER_ID);
        }

        @Test
        @DisplayName("returns zeroed averages when user has no sessions")
        void zeroedAveragesWhenNoSessions() {
            when(practiceSessionRepository.countByUserId(USER_ID)).thenReturn(0L);
            when(practiceSessionRepository.findByUserId(USER_ID)).thenReturn(List.of());

            Map<String, Object> result = service.getDashboardStats(USER_ID);

            assertThat(result.get("totalPractices")).isEqualTo(0L);
            assertThat((Double) result.get("avgAccuracy")).isEqualTo(0.0);
            assertThat((Double) result.get("avgWpm")).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("creates a new profile when none exists for the user")
        void createsNewProfileWhenMissing() {
            when(mcProfileRepository.findByUser(USER_ID)).thenReturn(Optional.empty());
            when(mcProfileRepository.save(org.mockito.ArgumentMatchers.any(MCProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MCProfile update = new MCProfile();
            update.setBiography("New bio");

            MCProfile result = service.updateProfile(USER_ID, update);

            assertThat(result.getUser()).isEqualTo(USER_ID);
            assertThat(result.getBiography()).isEqualTo("New bio");
        }

        @Test
        @DisplayName("experience is preserved when update payload's is left at its zero-default (not a real 'no value')")
        void partialUpdatePreservesExperience() {
            MCProfile existing = MCProfile.builder()
                    .user(USER_ID).experience(5).biography("Old bio")
                    .build();
            when(mcProfileRepository.findByUser(USER_ID)).thenReturn(Optional.of(existing));
            when(mcProfileRepository.save(org.mockito.ArgumentMatchers.any(MCProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MCProfile update = new MCProfile();
            update.setBiography("Updated bio");
            // experience left at 0 (default) — should NOT overwrite existing 5

            MCProfile result = service.updateProfile(USER_ID, update);

            assertThat(result.getBiography()).isEqualTo("Updated bio");
            assertThat(result.getExperience()).isEqualTo(5);
        }

        @Test
        @DisplayName("FINDING: personality/hostingStyle are silently wiped by a partial update — see DEFECT report. " +
                "MCProfile.personality/hostingStyle default to \"\" (not null), so the null-check guard in " +
                "updateProfile() never skips them; any partial update payload built via `new MCProfile()` " +
                "erases these fields even when the caller never intended to touch them.")
        void partialUpdateSilentlyWipesPersonalityAndHostingStyle() {
            MCProfile existing = MCProfile.builder()
                    .user(USER_ID).biography("Old bio")
                    .personality("Cheerful").hostingStyle("Formal")
                    .build();
            when(mcProfileRepository.findByUser(USER_ID)).thenReturn(Optional.of(existing));
            when(mcProfileRepository.save(org.mockito.ArgumentMatchers.any(MCProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MCProfile update = new MCProfile();
            update.setBiography("Updated bio");
            // caller never touches personality/hostingStyle — but they get wiped anyway

            MCProfile result = service.updateProfile(USER_ID, update);

            assertThat(result.getPersonality()).isEmpty();
            assertThat(result.getHostingStyle()).isEmpty();
        }

        @Test
        @DisplayName("updates experience when a positive value is supplied")
        void updatesExperienceWhenPositive() {
            MCProfile existing = MCProfile.builder().user(USER_ID).experience(2).build();
            when(mcProfileRepository.findByUser(USER_ID)).thenReturn(Optional.of(existing));
            when(mcProfileRepository.save(org.mockito.ArgumentMatchers.any(MCProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MCProfile update = new MCProfile();
            update.setExperience(10);

            MCProfile result = service.updateProfile(USER_ID, update);

            assertThat(result.getExperience()).isEqualTo(10);
        }

        @Test
        @DisplayName("replaces languages/styles lists only when the new list is non-empty")
        void replacesListsOnlyWhenNonEmpty() {
            MCProfile existing = MCProfile.builder()
                    .user(USER_ID)
                    .languages(List.of("Vietnamese"))
                    .styles(List.of("Wedding"))
                    .build();
            when(mcProfileRepository.findByUser(USER_ID)).thenReturn(Optional.of(existing));
            when(mcProfileRepository.save(org.mockito.ArgumentMatchers.any(MCProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MCProfile update = new MCProfile();
            update.setLanguages(List.of()); // empty — should NOT overwrite
            update.setStyles(List.of("Corporate", "Gala")); // non-empty — should overwrite

            MCProfile result = service.updateProfile(USER_ID, update);

            assertThat(result.getLanguages()).containsExactly("Vietnamese");
            assertThat(result.getStyles()).containsExactly("Corporate", "Gala");
        }
    }
}
