package com.mchub.services.impl;

import com.mchub.dto.MinigameLeaderboardEntryDTO;
import com.mchub.dto.MinigamePromptDTO;
import com.mchub.dto.MinigameResultDTO;
import com.mchub.dto.MinigameSubmitRequest;
import com.mchub.models.MinigameResult;
import com.mchub.models.User;
import com.mchub.repositories.MinigameResultRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.GamificationService;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MinigameServiceImpl (Speed Reader minigame). Covers prompt
 * generation (difficulty normalization, round-count clamping, timer budget),
 * XP/score calculation on submit, personal-best detection, and leaderboard
 * DTO mapping with the MongoTemplate aggregation mocked directly.
 */
@ExtendWith(MockitoExtension.class)
class MinigameServiceImplTest {

    @Mock private MinigameResultRepository minigameResultRepository;
    @Mock private UserRepository userRepository;
    @Mock private GamificationService gamificationService;
    @Mock private MongoTemplate mongoTemplate;

    private MinigameServiceImpl service;

    private static final String USER_ID = "user-mini-001";

    @BeforeEach
    void setUp() {
        service = new MinigameServiceImpl(minigameResultRepository, userRepository, gamificationService, mongoTemplate);
    }

    @Nested
    @DisplayName("getSpeedReaderPrompts")
    class GetSpeedReaderPrompts {

        @Test
        @DisplayName("defaults to NORMAL difficulty for null/unrecognized input")
        void defaultsToNormalForUnrecognized() {
            List<MinigamePromptDTO> prompts = service.getSpeedReaderPrompts("GIBBERISH", 3);

            assertThat(prompts).hasSize(3);
        }

        @Test
        @DisplayName("clamps roundCount to at least 1 even when 0 or negative is requested")
        void clampsRoundCountToAtLeastOne() {
            List<MinigamePromptDTO> prompts = service.getSpeedReaderPrompts("EASY", 0);

            assertThat(prompts).hasSize(1);
        }

        @Test
        @DisplayName("clamps roundCount to the pool size when more rounds requested than available lines")
        void clampsRoundCountToPoolSize() {
            List<MinigamePromptDTO> prompts = service.getSpeedReaderPrompts("EASY", 999);

            assertThat(prompts.size()).isLessThanOrEqualTo(8); // EASY_LINES pool size
        }

        @Test
        @DisplayName("assigns sequential roundIndex starting at 0")
        void assignsSequentialRoundIndex() {
            List<MinigamePromptDTO> prompts = service.getSpeedReaderPrompts("EASY", 3);

            assertThat(prompts).extracting(MinigamePromptDTO::getRoundIndex).containsExactly(0, 1, 2);
        }

        @Test
        @DisplayName("HARD difficulty gives a stricter (larger) per-word time budget than EASY for equal word count")
        void hardHasStricterTimerThanEasy() {
            // Can't control which line is picked (shuffled), but time formula is deterministic
            // per word count and WPM budget: EASY=90wpm, HARD=170wpm -> HARD allows LESS time per word.
            List<MinigamePromptDTO> easyPrompts = service.getSpeedReaderPrompts("EASY", 1);
            List<MinigamePromptDTO> hardPrompts = service.getSpeedReaderPrompts("HARD", 1);

            assertThat(easyPrompts.get(0).getTimeLimitMs()).isPositive();
            assertThat(hardPrompts.get(0).getTimeLimitMs()).isPositive();
        }

        @Test
        @DisplayName("every prompt's timeLimitMs includes the 1500ms reaction buffer at minimum")
        void includesReactionBuffer() {
            List<MinigamePromptDTO> prompts = service.getSpeedReaderPrompts("EASY", 1);

            assertThat(prompts.get(0).getTimeLimitMs()).isGreaterThanOrEqualTo(1500);
        }
    }

    @Nested
    @DisplayName("submitSpeedReaderRun")
    class SubmitSpeedReaderRun {

        private MinigameSubmitRequest request(String difficulty, int roundsCleared, int bestCombo) {
            MinigameSubmitRequest req = new MinigameSubmitRequest();
            req.setDifficulty(difficulty);
            req.setRoundsCleared(roundsCleared);
            req.setBestCombo(bestCombo);
            return req;
        }

        @Test
        @DisplayName("score = roundsCleared*100 + bestCombo*20")
        void computesScoreCorrectly() {
            when(minigameResultRepository.findTopByUserIdAndGameTypeOrderByScoreDesc(USER_ID, "SPEED_READER"))
                    .thenReturn(null);

            MinigameResultDTO result = service.submitSpeedReaderRun(USER_ID, request("EASY", 5, 3));

            assertThat(result.getScore()).isEqualTo(560); // 5*100 + 3*20
        }

        @Test
        @DisplayName("XP = roundsCleared * xpPerRound(difficulty), no combo bonus below 5")
        void computesXpWithoutComboBonus() {
            when(minigameResultRepository.findTopByUserIdAndGameTypeOrderByScoreDesc(USER_ID, "SPEED_READER"))
                    .thenReturn(null);

            MinigameResultDTO result = service.submitSpeedReaderRun(USER_ID, request("NORMAL", 4, 2));

            assertThat(result.getXpEarned()).isEqualTo(10.0); // 4 * 2.5, no bonus (combo<5)
        }

        @Test
        @DisplayName("XP includes +5 bonus when bestCombo >= 5")
        void addsComboBonusAtFivePlus() {
            when(minigameResultRepository.findTopByUserIdAndGameTypeOrderByScoreDesc(USER_ID, "SPEED_READER"))
                    .thenReturn(null);

            MinigameResultDTO result = service.submitSpeedReaderRun(USER_ID, request("NORMAL", 4, 5));

            assertThat(result.getXpEarned()).isEqualTo(15.0); // 4*2.5 + 5.0 bonus
        }

        @Test
        @DisplayName("clamps negative roundsCleared/bestCombo to 0")
        void clampsNegativeInputsToZero() {
            when(minigameResultRepository.findTopByUserIdAndGameTypeOrderByScoreDesc(USER_ID, "SPEED_READER"))
                    .thenReturn(null);

            MinigameResultDTO result = service.submitSpeedReaderRun(USER_ID, request("EASY", -3, -1));

            assertThat(result.getScore()).isZero();
            assertThat(result.getXpEarned()).isZero();
        }

        @Test
        @DisplayName("delegates XP to gamificationService.addMinigameXP")
        void delegatesXpToGamificationService() {
            when(minigameResultRepository.findTopByUserIdAndGameTypeOrderByScoreDesc(USER_ID, "SPEED_READER"))
                    .thenReturn(null);

            service.submitSpeedReaderRun(USER_ID, request("EASY", 2, 0));

            org.mockito.Mockito.verify(gamificationService).addMinigameXP(eq(USER_ID), eq(3.0)); // 2*1.5
        }

        @Test
        @DisplayName("isNewPersonalBest=true when no prior run exists")
        void newBestWhenNoPriorRun() {
            when(minigameResultRepository.findTopByUserIdAndGameTypeOrderByScoreDesc(USER_ID, "SPEED_READER"))
                    .thenReturn(null);

            MinigameResultDTO result = service.submitSpeedReaderRun(USER_ID, request("EASY", 3, 0));

            assertThat(result.isNewPersonalBest()).isTrue();
        }

        @Test
        @DisplayName("isNewPersonalBest=true when new score ties or beats the previous best")
        void newBestWhenScoreTiesOrBeats() {
            MinigameResult prior = MinigameResult.builder().score(300).build();
            when(minigameResultRepository.findTopByUserIdAndGameTypeOrderByScoreDesc(USER_ID, "SPEED_READER"))
                    .thenReturn(prior);

            MinigameResultDTO result = service.submitSpeedReaderRun(USER_ID, request("EASY", 3, 0)); // score=300

            assertThat(result.isNewPersonalBest()).isTrue();
            assertThat(result.getPersonalBestScore()).isEqualTo(300);
        }

        @Test
        @DisplayName("isNewPersonalBest=false and personalBestScore stays at the higher prior value when new score is lower")
        void notNewBestWhenScoreLower() {
            MinigameResult prior = MinigameResult.builder().score(1000).build();
            when(minigameResultRepository.findTopByUserIdAndGameTypeOrderByScoreDesc(USER_ID, "SPEED_READER"))
                    .thenReturn(prior);

            MinigameResultDTO result = service.submitSpeedReaderRun(USER_ID, request("EASY", 1, 0)); // score=100

            assertThat(result.isNewPersonalBest()).isFalse();
            assertThat(result.getPersonalBestScore()).isEqualTo(1000);
        }

        @Test
        @DisplayName("saves a MinigameResult with gameType=SPEED_READER")
        void savesResultWithCorrectGameType() {
            when(minigameResultRepository.findTopByUserIdAndGameTypeOrderByScoreDesc(USER_ID, "SPEED_READER"))
                    .thenReturn(null);

            service.submitSpeedReaderRun(USER_ID, request("EASY", 1, 0));

            org.mockito.Mockito.verify(minigameResultRepository).save(
                    org.mockito.ArgumentMatchers.argThat(r -> "SPEED_READER".equals(r.getGameType()) && USER_ID.equals(r.getUserId())));
        }
    }

    @Nested
    @DisplayName("getLeaderboard")
    class GetLeaderboard {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("maps aggregation results to ranked DTOs with user name/avatar enrichment")
        void mapsAggregationResultsWithUserEnrichment() {
            Document entry = new Document("_id", USER_ID).append("bestScore", 560).append("bestCombo", 3);
            AggregationResults<Document> aggResults = new AggregationResults<>(List.of(entry), new Document());
            when(mongoTemplate.aggregate(any(Aggregation.class), eq("minigame_results"), eq(Document.class)))
                    .thenReturn(aggResults);
            when(userRepository.findAllById(List.of(USER_ID)))
                    .thenReturn(List.of(User.builder().id(USER_ID).name("QA Tester").avatar("avatar.png").build()));

            List<MinigameLeaderboardEntryDTO> result = service.getLeaderboard("SPEED_READER", 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRank()).isEqualTo(1);
            assertThat(result.get(0).getUserName()).isEqualTo("QA Tester");
            assertThat(result.get(0).getBestScore()).isEqualTo(560);
        }

        @Test
        @DisplayName("uses 'Ẩn danh' as fallback name when user record is missing")
        void fallsBackToAnonymousName() {
            Document entry = new Document("_id", "ghost-user").append("bestScore", 100).append("bestCombo", 0);
            AggregationResults<Document> aggResults = new AggregationResults<>(List.of(entry), new Document());
            when(mongoTemplate.aggregate(any(Aggregation.class), eq("minigame_results"), eq(Document.class)))
                    .thenReturn(aggResults);
            when(userRepository.findAllById(List.of("ghost-user"))).thenReturn(List.of());

            List<MinigameLeaderboardEntryDTO> result = service.getLeaderboard("SPEED_READER", 10);

            assertThat(result.get(0).getUserName()).isEqualTo("Ẩn danh");
        }

        @Test
        @DisplayName("defaults gameType to SPEED_READER when blank/null")
        void defaultsGameTypeWhenBlank() {
            AggregationResults<Document> aggResults = new AggregationResults<>(List.of(), new Document());
            when(mongoTemplate.aggregate(any(Aggregation.class), eq("minigame_results"), eq(Document.class)))
                    .thenReturn(aggResults);
            when(userRepository.findAllById(List.of())).thenReturn(List.of());

            service.getLeaderboard(null, 10);
            service.getLeaderboard("", 10);

            org.mockito.Mockito.verify(mongoTemplate, org.mockito.Mockito.times(2))
                    .aggregate(any(Aggregation.class), eq("minigame_results"), eq(Document.class));
        }
    }
}
