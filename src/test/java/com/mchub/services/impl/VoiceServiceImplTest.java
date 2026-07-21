package com.mchub.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchub.config.PlanConfig;
import com.mchub.dto.PracticeSessionResponseDTO;
import com.mchub.dto.VoiceLessonResponseDTO;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.VoiceLessonCategory;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.mapper.PracticeSessionMapper;
import com.mchub.mapper.VoiceLessonMapper;
import com.mchub.models.LessonAdaptiveStats;
import com.mchub.models.PracticeSession;
import com.mchub.models.User;
import com.mchub.models.VoiceLesson;
import com.mchub.repositories.LessonAdaptiveStatsRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.VoiceLessonRepository;
import com.mchub.services.AdaptiveCalibrationService;
import com.mchub.services.GamificationService;
import com.mchub.services.MediaService;
import com.mchub.services.VoiceLessonSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for VoiceServiceImpl. Mocks all repositories/external services.
 * Tests the plan-expiry-downgrade logic, session-limit enforcement per plan,
 * practice-analysis flow, and CRUD operations.
 */
@ExtendWith(MockitoExtension.class)
class VoiceServiceImplTest {

    @Mock private VoiceLessonRepository lessonRepository;
    @Mock private PracticeSessionRepository sessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private VoiceLessonMapper lessonMapper;
    @Mock private PracticeSessionMapper sessionMapper;
    @Mock private MediaService mediaService;
    @Mock private VoiceLessonSearchService lessonSearchService;
    @Mock private RestTemplate restTemplate;
    @Mock private GamificationService gamificationService;
    @Mock private AdaptiveCalibrationService adaptiveCalibrationService;
    @Mock private LessonAdaptiveStatsRepository adaptiveStatsRepository;

    private VoiceServiceImpl voiceService;

    private static final String USER_ID = "user-voice-001";
    private static final String LESSON_ID = "lesson-001";
    private static final String AI_SERVICE_URL = "http://ai-service/analyze-voice";

    @BeforeEach
    void setUp() {
        voiceService = new VoiceServiceImpl(
                lessonRepository, sessionRepository, userRepository,
                lessonMapper, sessionMapper, mediaService, lessonSearchService,
                restTemplate, gamificationService, new ObjectMapper(),
                adaptiveCalibrationService, adaptiveStatsRepository);
        ReflectionTestUtils.setField(voiceService, "aiServiceUrl", AI_SERVICE_URL);
    }

    private User freeUser() {
        return User.builder().id(USER_ID).plan(SubscriptionPlan.FREE).build();
    }

    private User basicUser() {
        return User.builder().id(USER_ID).plan(SubscriptionPlan.BASIC)
                .aiSessionsUsed(0).planExpiresAt(LocalDateTime.now().plusDays(20))
                .isPremium(true).build();
    }

    private User dailyUser() {
        return User.builder().id(USER_ID).plan(SubscriptionPlan.DAILY)
                .aiSessionsUsed(0).planExpiresAt(LocalDateTime.now().plusDays(1))
                .isPremium(true).build();
    }

    private VoiceLesson anyLesson() {
        return VoiceLesson.builder().id(LESSON_ID).title("Test Lesson")
                .content("Practice script content").category(VoiceLessonCategory.GENERAL)
                .targetWpmMin(140).targetWpmMax(180).evaluationCriteria(List.of())
                .practiceCount(0).build();
    }

    /** Builds a minimal successful AI-service response map — avoids Map.of()'s 10-pair limit. */
    private Map<String, Object> aiSuccessResponse() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "success");
        m.put("accuracy_score", 85.0);
        m.put("rhythm_score", 75.0);
        m.put("speaking_rate_wpm", 150.0);
        m.put("text_spoken", "hello");
        m.put("feedback_vi", "");
        m.put("feedback_en", "");
        m.put("report_vi", "");
        m.put("report_en", "");
        m.put("overall_score", 80.0);
        m.put("cer_rate", 0.1);
        m.put("wer_rate", 0.05);
        return m;
    }

    @Nested
    @DisplayName("analyzePractice — plan expiry downgrade")
    class AnalyzePracticePlanExpiry {

        @Test
        @DisplayName("downgrades user from BASIC to FREE when planExpiresAt is in the past")
        void downgradesExpiredPlan() {
            User expiredUser = User.builder().id(USER_ID).plan(SubscriptionPlan.BASIC)
                    .isPremium(true)
                    .planExpiresAt(LocalDateTime.now().minusDays(1)).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(expiredUser));
            when(sessionRepository.countByUserId(USER_ID)).thenReturn(0L);

            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());
            try {
                when(mediaService.uploadFile(audio, "practice_sessions")).thenReturn("http://cloudinary.com/audio.wav");
            } catch (Exception e) { /* mock never throws */ }

            Map<String, Object> aiResponse = aiSuccessResponse();

            when(restTemplate.postForObject(eq(AI_SERVICE_URL), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(aiResponse);

            PracticeSession savedSession = PracticeSession.builder().id("sess-1").build();
            when(sessionRepository.save(any(PracticeSession.class))).thenReturn(savedSession);
            when(lessonRepository.save(any(VoiceLesson.class))).thenReturn(lesson);
            when(sessionMapper.toResponseDTO(any(PracticeSession.class))).thenReturn(new PracticeSessionResponseDTO());

            voiceService.analyzePractice(LESSON_ID, USER_ID, audio);

            assertThat(expiredUser.getPlan()).isEqualTo(SubscriptionPlan.FREE);
            assertThat(expiredUser.isPremium()).isFalse();
            // saved twice: once for the expiry downgrade, once for the aiSessionsUsed++ (FREE plan)
            verify(userRepository, org.mockito.Mockito.times(2)).save(expiredUser);
        }
    }

    @Nested
    @DisplayName("analyzePractice — session limit enforcement")
    class AnalyzePracticeSessionLimit {

        @Test
        @DisplayName("FREE plan: blocks when session count reaches limit")
        void blocksFreePlanAtLimit() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(freeUser()));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(anyLesson()));
            when(sessionRepository.countByUserId(USER_ID)).thenReturn((long) PlanConfig.FREE_SESSION_LIMIT);

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());

            assertThatThrownBy(() -> voiceService.analyzePractice(LESSON_ID, USER_ID, audio))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("FREE plan: allows session when under limit")
        void allowsFreePlanUnderLimit() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(freeUser()));
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sessionRepository.countByUserId(USER_ID)).thenReturn(1L);

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());
            try { when(mediaService.uploadFile(any(), anyString())).thenReturn("url"); } catch (Exception e) {}

            Map<String, Object> aiResponse = aiSuccessResponse();
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(aiResponse);

            PracticeSession saved = PracticeSession.builder().id("sess-1").build();
            when(sessionRepository.save(any(PracticeSession.class))).thenReturn(saved);
            when(lessonRepository.save(any(VoiceLesson.class))).thenReturn(lesson);
            when(sessionMapper.toResponseDTO(any(PracticeSession.class))).thenReturn(new PracticeSessionResponseDTO());

            voiceService.analyzePractice(LESSON_ID, USER_ID, audio);
            verify(sessionRepository).save(any(PracticeSession.class));
        }

        @Test
        @DisplayName("BASIC plan: blocks when aiSessionsUsed reaches limit")
        void blocksBasicPlanAtLimit() {
            User basic = basicUser();
            basic.setAiSessionsUsed(PlanConfig.BASIC_AI_SESSION_LIMIT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(basic));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(anyLesson()));

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());

            assertThatThrownBy(() -> voiceService.analyzePractice(LESSON_ID, USER_ID, audio))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("DAILY plan: blocks when aiSessionsUsed reaches limit")
        void blocksDailyPlanAtLimit() {
            User daily = dailyUser();
            daily.setAiSessionsUsed(PlanConfig.DAILY_AI_SESSION_LIMIT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(daily));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(anyLesson()));

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());

            assertThatThrownBy(() -> voiceService.analyzePractice(LESSON_ID, USER_ID, audio))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("FULL plan: no limit, never throws LIMIT_EXCEEDED")
        void fullPlanNoLimit() {
            User fullUser = User.builder().id(USER_ID).plan(SubscriptionPlan.FULL)
                    .isPremium(true).planExpiresAt(LocalDateTime.now().plusDays(30))
                    .aiSessionsUsed(999).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(fullUser));
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());
            try { when(mediaService.uploadFile(any(), anyString())).thenReturn("url"); } catch (Exception e) {}

            Map<String, Object> aiResponse = aiSuccessResponse();
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(aiResponse);

            PracticeSession saved = PracticeSession.builder().id("sess-1").build();
            when(sessionRepository.save(any(PracticeSession.class))).thenReturn(saved);
            when(lessonRepository.save(any(VoiceLesson.class))).thenReturn(lesson);
            when(sessionMapper.toResponseDTO(any(PracticeSession.class))).thenReturn(new PracticeSessionResponseDTO());

            voiceService.analyzePractice(LESSON_ID, USER_ID, audio);
            verify(sessionRepository).save(any(PracticeSession.class));
        }
    }

    @Nested
    @DisplayName("analyzePractice — AI session counter increment")
    class AnalyzePracticeAiSessionCounter {

        @Test
        @DisplayName("increments aiSessionsUsed for FREE plan")
        void incrementsForFree() {
            User free = freeUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(free));
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sessionRepository.countByUserId(USER_ID)).thenReturn(0L);

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());
            try { when(mediaService.uploadFile(any(), anyString())).thenReturn("url"); } catch (Exception e) {}

            Map<String, Object> aiResponse = aiSuccessResponse();
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(aiResponse);

            when(sessionRepository.save(any(PracticeSession.class))).thenReturn(PracticeSession.builder().id("s-1").build());
            when(lessonRepository.save(any(VoiceLesson.class))).thenReturn(lesson);
            when(sessionMapper.toResponseDTO(any(PracticeSession.class))).thenReturn(new PracticeSessionResponseDTO());

            voiceService.analyzePractice(LESSON_ID, USER_ID, audio);

            assertThat(free.getAiSessionsUsed()).isEqualTo(1);
            verify(userRepository).save(free);
        }

        @Test
        @DisplayName("increments aiSessionsUsed for BASIC plan")
        void incrementsForBasic() {
            User basic = basicUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(basic));
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());
            try { when(mediaService.uploadFile(any(), anyString())).thenReturn("url"); } catch (Exception e) {}

            Map<String, Object> aiResponse = aiSuccessResponse();
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(aiResponse);

            when(sessionRepository.save(any(PracticeSession.class))).thenReturn(PracticeSession.builder().id("s-1").build());
            when(lessonRepository.save(any(VoiceLesson.class))).thenReturn(lesson);
            when(sessionMapper.toResponseDTO(any(PracticeSession.class))).thenReturn(new PracticeSessionResponseDTO());

            voiceService.analyzePractice(LESSON_ID, USER_ID, audio);

            assertThat(basic.getAiSessionsUsed()).isEqualTo(1);
        }

        @Test
        @DisplayName("does NOT increment aiSessionsUsed for FULL plan")
        void doesNotIncrementForFull() {
            User full = User.builder().id(USER_ID).plan(SubscriptionPlan.FULL)
                    .isPremium(true).planExpiresAt(LocalDateTime.now().plusDays(30))
                    .aiSessionsUsed(5).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(full));
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());
            try { when(mediaService.uploadFile(any(), anyString())).thenReturn("url"); } catch (Exception e) {}

            Map<String, Object> aiResponse = aiSuccessResponse();
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(aiResponse);

            when(sessionRepository.save(any(PracticeSession.class))).thenReturn(PracticeSession.builder().id("s-1").build());
            when(lessonRepository.save(any(VoiceLesson.class))).thenReturn(lesson);
            when(sessionMapper.toResponseDTO(any(PracticeSession.class))).thenReturn(new PracticeSessionResponseDTO());

            voiceService.analyzePractice(LESSON_ID, USER_ID, audio);

            assertThat(full.getAiSessionsUsed()).isEqualTo(5); // unchanged
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("analyzePractice — gamification / adaptive / practice count side effects")
    class AnalyzePracticeSideEffects {

        @Test
        @DisplayName("calls gamificationService.processPracticeSession after save")
        void callsGamification() {
            User user = freeUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sessionRepository.countByUserId(USER_ID)).thenReturn(0L);

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());
            try { when(mediaService.uploadFile(any(), anyString())).thenReturn("url"); } catch (Exception e) {}

            Map<String, Object> aiResponse = aiSuccessResponse();
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(aiResponse);

            when(sessionRepository.save(any(PracticeSession.class))).thenAnswer(inv -> {
                PracticeSession ps = inv.getArgument(0);
                return PracticeSession.builder().id("s-1").accuracyScore(ps.getAccuracyScore())
                        .rhythmScore(ps.getRhythmScore()).overallScore(ps.getOverallScore()).build();
            });
            when(lessonRepository.save(any(VoiceLesson.class))).thenReturn(lesson);
            when(sessionMapper.toResponseDTO(any(PracticeSession.class))).thenReturn(new PracticeSessionResponseDTO());

            voiceService.analyzePractice(LESSON_ID, USER_ID, audio);

            verify(gamificationService).processPracticeSession(eq(USER_ID), eq(LESSON_ID), eq(85.0), eq(75.0), eq(80.0), eq(0.0));
        }

        @Test
        @DisplayName("increments lesson practice count")
        void incrementsPracticeCount() {
            User user = freeUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sessionRepository.countByUserId(USER_ID)).thenReturn(0L);

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());
            try { when(mediaService.uploadFile(any(), anyString())).thenReturn("url"); } catch (Exception e) {}

            Map<String, Object> aiResponse = aiSuccessResponse();
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(aiResponse);

            when(sessionRepository.save(any(PracticeSession.class))).thenReturn(PracticeSession.builder().id("s-1").build());
            when(lessonRepository.save(any(VoiceLesson.class))).thenReturn(lesson);
            when(sessionMapper.toResponseDTO(any(PracticeSession.class))).thenReturn(new PracticeSessionResponseDTO());

            assertThat(lesson.getPracticeCount()).isZero();

            voiceService.analyzePractice(LESSON_ID, USER_ID, audio);

            assertThat(lesson.getPracticeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("calls adaptiveCalibrationService.calibrateLesson after save")
        void callsAdaptiveCalibration() {
            User user = freeUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sessionRepository.countByUserId(USER_ID)).thenReturn(0L);

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());
            try { when(mediaService.uploadFile(any(), anyString())).thenReturn("url"); } catch (Exception e) {}

            Map<String, Object> aiResponse = aiSuccessResponse();
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(aiResponse);

            when(sessionRepository.save(any(PracticeSession.class))).thenReturn(PracticeSession.builder().id("s-1").build());
            when(lessonRepository.save(any(VoiceLesson.class))).thenReturn(lesson);
            when(sessionMapper.toResponseDTO(any(PracticeSession.class))).thenReturn(new PracticeSessionResponseDTO());

            voiceService.analyzePractice(LESSON_ID, USER_ID, audio);

            verify(adaptiveCalibrationService).calibrateLesson(LESSON_ID);
        }

        @Test
        @DisplayName("gamification failure does not propagate — logged and swallowed")
        void gamificationFailureSwallowed() {
            User user = freeUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(sessionRepository.countByUserId(USER_ID)).thenReturn(0L);

            MultipartFile audio = new MockMultipartFile("audio", "test.wav", "audio/wav", "fake".getBytes());
            try { when(mediaService.uploadFile(any(), anyString())).thenReturn("url"); } catch (Exception e) {}

            Map<String, Object> aiResponse = aiSuccessResponse();
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(aiResponse);

            when(sessionRepository.save(any(PracticeSession.class))).thenReturn(PracticeSession.builder().id("s-1").build());
            when(lessonRepository.save(any(VoiceLesson.class))).thenReturn(lesson);
            when(sessionMapper.toResponseDTO(any(PracticeSession.class))).thenReturn(new PracticeSessionResponseDTO());

            when(gamificationService.processPracticeSession(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenThrow(new RuntimeException("Gamification service down"));

            // Should not propagate — just log
            voiceService.analyzePractice(LESSON_ID, USER_ID, audio);
            // If we get here without exception, the fire-and-forget works
        }
    }

    @Nested
    @DisplayName("CRUD operations")
    class CrudOperations {

        @Test
        @DisplayName("createLesson saves lesson, indexes in search, returns DTO")
        void createsLesson() {
            when(lessonRepository.save(any(VoiceLesson.class))).thenAnswer(inv -> {
                VoiceLesson l = inv.getArgument(0);
                return VoiceLesson.builder().id("new-id").title(l.getTitle()).build();
            });
            when(lessonMapper.toResponseDTO(any(VoiceLesson.class)))
                    .thenReturn(VoiceLessonResponseDTO.builder().id("new-id").title("Test").build());

            VoiceLessonResponseDTO result = voiceService.createLesson(
                    "Test", "Content", VoiceLessonCategory.GENERAL,
                    "BEGINNER", "Desc", null, null, List.of(), null, 140, 180, 70);

            assertThat(result.getId()).isEqualTo("new-id");
            verify(lessonSearchService).indexLesson(any(VoiceLesson.class));
        }

        @Test
        @DisplayName("deleteLesson soft-deletes by setting isActive=false")
        void softDeletesLesson() {
            VoiceLesson lesson = VoiceLesson.builder().id(LESSON_ID).isActive(true).build();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            voiceService.deleteLesson(LESSON_ID);

            assertThat(lesson.isActive()).isFalse();
            verify(lessonRepository).save(lesson);
        }

        @Test
        @DisplayName("deleteLesson throws RESOURCE_NOT_FOUND for missing id")
        void deleteThrowsOnMissing() {
            when(lessonRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> voiceService.deleteLesson("missing"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("getLessonById returns mapped DTO")
        void getsLessonById() {
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
            when(lessonMapper.toResponseDTO(lesson)).thenReturn(VoiceLessonResponseDTO.builder().id(LESSON_ID).build());

            VoiceLessonResponseDTO result = voiceService.getLessonById(LESSON_ID);

            assertThat(result.getId()).isEqualTo(LESSON_ID);
        }

        @Test
        @DisplayName("getLessonById throws USER_NOT_FOUND (misleading name but follows code) for missing")
        void getLessonByIdThrowsOnMissing() {
            when(lessonRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> voiceService.getLessonById("missing"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("getAllLessons returns only active lessons")
        void getAllLessonsActiveOnly() {
            VoiceLesson active = anyLesson();
            when(lessonRepository.findByIsActiveTrue()).thenReturn(List.of(active));
            when(lessonMapper.toResponseDTO(active)).thenReturn(VoiceLessonResponseDTO.builder().id(LESSON_ID).build());

            List<VoiceLessonResponseDTO> result = voiceService.getAllLessons();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getFeaturedLessons limits by practice count descending")
        void getFeaturedLessons() {
            when(lessonRepository.findByPracticeCountGreaterThanOrderByPracticeCountDesc(eq(0), any(PageRequest.class)))
                    .thenReturn(List.of(anyLesson()));
            when(lessonMapper.toResponseDTO(any())).thenReturn(VoiceLessonResponseDTO.builder().build());

            List<VoiceLessonResponseDTO> result = voiceService.getFeaturedLessons(5);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getAdaptiveStats returns stats when found, null otherwise")
        void getAdaptiveStats() {
            LessonAdaptiveStats stats = new LessonAdaptiveStats();
            when(adaptiveStatsRepository.findByLessonId(LESSON_ID)).thenReturn(Optional.of(stats));
            assertThat(voiceService.getAdaptiveStats(LESSON_ID)).isSameAs(stats);

            when(adaptiveStatsRepository.findByLessonId("missing")).thenReturn(Optional.empty());
            assertThat(voiceService.getAdaptiveStats("missing")).isNull();
        }
    }

    @Nested
    @DisplayName("userPracticeHistory / getPracticeSessionById")
    class History {

        @Test
        @DisplayName("getUserPracticeHistory fetches lesson titles in batch")
        void getsPracticeHistory() {
            PracticeSession sess = PracticeSession.builder().id("s-1").lessonId(LESSON_ID).userId(USER_ID).build();
            when(sessionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(sess));
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findAllById(List.of(LESSON_ID))).thenReturn(List.of(lesson));

            PracticeSessionResponseDTO dto = new PracticeSessionResponseDTO();
            when(sessionMapper.toResponseDTO(sess)).thenReturn(dto);

            List<PracticeSessionResponseDTO> result = voiceService.getUserPracticeHistory(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(dto.getLessonTitle()).isEqualTo(lesson.getTitle());
        }

        @Test
        @DisplayName("getPracticeSessionById enriches DTO with lesson title when found")
        void getSessionByIdWithLesson() {
            PracticeSession sess = PracticeSession.builder().id("s-1").lessonId(LESSON_ID).build();
            when(sessionRepository.findById("s-1")).thenReturn(Optional.of(sess));
            VoiceLesson lesson = anyLesson();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            PracticeSessionResponseDTO dto = new PracticeSessionResponseDTO();
            when(sessionMapper.toResponseDTO(sess)).thenReturn(dto);

            PracticeSessionResponseDTO result = voiceService.getPracticeSessionById("s-1");

            assertThat(result).isSameAs(dto);
            assertThat(dto.getLessonTitle()).isEqualTo("Test Lesson");
        }
    }
}
