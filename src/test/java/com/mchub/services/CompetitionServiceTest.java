package com.mchub.services;

import com.mchub.enums.CompetitionType;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Competition;
import com.mchub.repositories.CompetitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CompetitionService. Regression guard for the audit fix
 * (Remaining_Modules_Audit_Report.md 2.5): AdminCompetitionController used to
 * bypass this service entirely and call the repository directly, and
 * deleteCompetition() used to silently no-op on an unknown id (Spring Data
 * deleteById does not throw). Both are covered below.
 */
@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    @Mock private CompetitionRepository competitionRepository;

    private CompetitionService competitionService;

    private static final String COMP_ID = "comp-001";

    @BeforeEach
    void setUp() {
        competitionService = new CompetitionService(competitionRepository);
    }

    @Nested
    @DisplayName("createCompetition")
    class CreateCompetition {

        @Test
        @DisplayName("defaults startDate to now when not supplied")
        void defaultsStartDate() {
            Competition comp = Competition.builder().title("Weekly Arena").type(CompetitionType.WEEKLY).build();
            when(competitionRepository.save(any(Competition.class))).thenAnswer(inv -> inv.getArgument(0));

            Competition result = competitionService.createCompetition(comp);

            assertThat(result.getStartDate()).isNotNull();
            assertThat(result.getStartDate()).isCloseTo(Instant.now(), within3Seconds());
        }

        @Test
        @DisplayName("defaults endDate to startDate+7 days when not supplied")
        void defaultsEndDateToSevenDaysLater() {
            Competition comp = Competition.builder().title("Weekly Arena").type(CompetitionType.WEEKLY).build();
            when(competitionRepository.save(any(Competition.class))).thenAnswer(inv -> inv.getArgument(0));

            Competition result = competitionService.createCompetition(comp);

            assertThat(result.getEndDate()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));
        }

        @Test
        @DisplayName("preserves explicit startDate/endDate when supplied")
        void preservesExplicitDates() {
            Instant start = Instant.now().minus(10, ChronoUnit.DAYS);
            Instant end = Instant.now().plus(3, ChronoUnit.DAYS);
            Competition comp = Competition.builder().title("Custom").type(CompetitionType.DAILY)
                    .startDate(start).endDate(end).build();
            when(competitionRepository.save(any(Competition.class))).thenAnswer(inv -> inv.getArgument(0));

            Competition result = competitionService.createCompetition(comp);

            assertThat(result.getStartDate()).isEqualTo(start);
            assertThat(result.getEndDate()).isEqualTo(end);
        }

        private org.assertj.core.data.TemporalUnitOffset within3Seconds() {
            return org.assertj.core.api.Assertions.within(3, ChronoUnit.SECONDS);
        }
    }

    @Nested
    @DisplayName("updateCompetition")
    class UpdateCompetition {

        @Test
        @DisplayName("overwrites all mutable fields")
        void overwritesFields() {
            Competition existing = Competition.builder().id(COMP_ID).title("Old").active(true).build();
            when(competitionRepository.findById(COMP_ID)).thenReturn(Optional.of(existing));
            when(competitionRepository.save(any(Competition.class))).thenAnswer(inv -> inv.getArgument(0));

            Competition update = Competition.builder().title("New Title").description("New desc")
                    .type(CompetitionType.MONTHLY).active(false).build();

            Competition result = competitionService.updateCompetition(COMP_ID, update);

            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.getDescription()).isEqualTo("New desc");
            assertThat(result.getType()).isEqualTo(CompetitionType.MONTHLY);
            assertThat(result.isActive()).isFalse();
        }

        @Test
        @DisplayName("throws AppException(RESOURCE_NOT_FOUND) — not IllegalArgumentException — for unknown id")
        void throwsAppExceptionForUnknownId() {
            when(competitionRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> competitionService.updateCompetition("missing", Competition.builder().build()))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteCompetition — regression guard for silent no-op fix")
    class DeleteCompetition {

        @Test
        @DisplayName("deletes when the competition exists")
        void deletesWhenExists() {
            when(competitionRepository.existsById(COMP_ID)).thenReturn(true);

            competitionService.deleteCompetition(COMP_ID);

            verify(competitionRepository).deleteById(COMP_ID);
        }

        @Test
        @DisplayName("throws RESOURCE_NOT_FOUND instead of silently no-op-ing for an unknown id")
        void throwsInsteadOfSilentNoOp() {
            when(competitionRepository.existsById("missing")).thenReturn(false);

            assertThatThrownBy(() -> competitionService.deleteCompetition("missing"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

            verify(competitionRepository, never()).deleteById(any());
        }
    }
}
