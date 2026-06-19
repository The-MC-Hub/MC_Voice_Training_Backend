package com.mchub.services.impl;

import com.mchub.dto.PracticeSessionResponseDTO;
import com.mchub.dto.VoiceLessonResponseDTO;
import com.mchub.enums.VoiceLessonCategory;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.mapper.PracticeSessionMapper;
import com.mchub.mapper.VoiceLessonMapper;
import com.mchub.models.PracticeSession;
import com.mchub.models.VoiceLesson;
import com.mchub.config.PlanConfig;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.repositories.LessonAdaptiveStatsRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.VoiceLessonRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.models.User;
import com.mchub.services.AdaptiveCalibrationService;
import com.mchub.services.MediaService;
import com.mchub.services.VoiceLessonSearchService;
import com.mchub.services.VoiceService;
import com.mchub.services.GamificationService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceServiceImpl implements VoiceService {

    private final VoiceLessonRepository lessonRepository;
    private final PracticeSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final VoiceLessonMapper lessonMapper;
    private final PracticeSessionMapper sessionMapper;
    private final MediaService mediaService;
    private final VoiceLessonSearchService lessonSearchService;
    private final RestTemplate restTemplate;
    private final GamificationService gamificationService;
    private final ObjectMapper objectMapper;
    private final AdaptiveCalibrationService adaptiveCalibrationService;
    private final LessonAdaptiveStatsRepository adaptiveStatsRepository;

    @Value("${ai.service.analyze-url:http://127.0.0.1:8001/analyze-voice}")
    private String AI_SERVICE_URL;

    @Value("${ai.service.tts-url:http://127.0.0.1:8001/generate-mc-voice}")
    private String AI_TTS_SERVICE_URL;

    @Override
    public VoiceLessonResponseDTO createLesson(String title, String content, VoiceLessonCategory category,
            String difficulty, String description, MultipartFile thumbnail, String videoUrl,
            List<VoiceLesson.EvaluationCriteria> evaluationCriteria, String evaluationHint,
            int targetWpmMin, int targetWpmMax, int passingScore) {
        String thumbnailUrl = "";
        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                thumbnailUrl = mediaService.uploadFile(thumbnail, "voice_lessons");
            } catch (IOException e) {
                log.error("Failed to upload thumbnail", e);
            }
        }

        VoiceLesson lesson = VoiceLesson.builder()
                .title(title)
                .content(content)
                .category(category)
                .difficulty(difficulty)
                .description(description)
                .thumbnailUrl(thumbnailUrl)
                .videoUrl(videoUrl)
                .evaluationCriteria(evaluationCriteria != null ? evaluationCriteria : new java.util.ArrayList<>())
                .evaluationHint(evaluationHint)
                .targetWpmMin(targetWpmMin)
                .targetWpmMax(targetWpmMax)
                .passingScore(passingScore)
                .build();

        VoiceLesson savedLesson = lessonRepository.save(lesson);
        lessonSearchService.indexLesson(savedLesson);
        return lessonMapper.toResponseDTO(savedLesson);
    }

    @Override
    public VoiceLessonResponseDTO updateLesson(String id, String title, String content, VoiceLessonCategory category,
            String difficulty, String description, MultipartFile thumbnail, String videoUrl,
            List<VoiceLesson.EvaluationCriteria> evaluationCriteria, String evaluationHint,
            int targetWpmMin, int targetWpmMax, int passingScore) {
        VoiceLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Lesson not found"));

        lesson.setTitle(title);
        lesson.setContent(content);
        lesson.setCategory(category);
        lesson.setDifficulty(difficulty);
        lesson.setDescription(description);
        lesson.setEvaluationCriteria(evaluationCriteria != null ? evaluationCriteria : new java.util.ArrayList<>());
        lesson.setEvaluationHint(evaluationHint);
        lesson.setTargetWpmMin(targetWpmMin);
        lesson.setTargetWpmMax(targetWpmMax);
        lesson.setPassingScore(passingScore);
        lesson.setVideoUrl(videoUrl);

        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                String thumbnailUrl = mediaService.uploadFile(thumbnail, "voice_lessons");
                lesson.setThumbnailUrl(thumbnailUrl);
            } catch (IOException e) {
                log.error("Failed to upload thumbnail during update", e);
            }
        }

        return lessonMapper.toResponseDTO(lessonRepository.save(lesson));
    }

    @Override
    public List<VoiceLessonResponseDTO> getAllLessons() {
        return lessonRepository.findByIsActiveTrue().stream()
                .map(lessonMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoiceLessonResponseDTO> searchLessons(String searchTerm, VoiceLessonCategory category) {
        return lessonSearchService.searchLessons(searchTerm, category).stream()
                .map(lessonMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteLesson(String id) {
        VoiceLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new AppException(com.mchub.exception.ErrorCode.RESOURCE_NOT_FOUND, "Lesson not found: " + id));
        lesson.setActive(false);
        lessonRepository.save(lesson);
    }

    @Override
    public List<VoiceLessonResponseDTO> getLessonsByCategory(VoiceLessonCategory category) {
        return lessonRepository.findByCategoryAndIsActiveTrue(category).stream()
                .map(lessonMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public VoiceLessonResponseDTO getLessonById(String id) {
        VoiceLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Lesson not found"));
        return lessonMapper.toResponseDTO(lesson);
    }

    @Override
    public PracticeSessionResponseDTO analyzePractice(String lessonId, String userId, MultipartFile audioFile) {
        // 0. Limit checking (5 free practice attempts)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        SubscriptionPlan plan = user.getPlan() != null ? user.getPlan() : SubscriptionPlan.FREE;

        // Check plan expiry — downgrade to FREE if expired
        if (plan != SubscriptionPlan.FREE && user.getPlanExpiresAt() != null
                && user.getPlanExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            plan = SubscriptionPlan.FREE;
            user.setPlan(SubscriptionPlan.FREE);
            user.setPremium(false);
            userRepository.save(user);
        }

        VoiceLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Lesson not found"));

        if (plan == SubscriptionPlan.FREE) {
            long count = sessionRepository.countByUserId(userId);
            if (count >= PlanConfig.FREE_SESSION_LIMIT) {
                throw new AppException(ErrorCode.LIMIT_EXCEEDED,
                        "Free plan limit: " + PlanConfig.FREE_SESSION_LIMIT + " sessions. Upgrade to continue.");
            }
        } else if (plan == SubscriptionPlan.DAILY) {
            if (user.getAiSessionsUsed() >= PlanConfig.DAILY_AI_SESSION_LIMIT) {
                throw new AppException(ErrorCode.LIMIT_EXCEEDED,
                        "Goi Ngay: da dung het " + PlanConfig.DAILY_AI_SESSION_LIMIT + " AI sessions. Gia han them 1 ngay de tiep tuc.");
            }
        } else if (plan == SubscriptionPlan.BASIC) {
            // BASIC: all categories allowed, max 20 AI sessions/month
            if (user.getAiSessionsUsed() >= PlanConfig.BASIC_AI_SESSION_LIMIT) {
                throw new AppException(ErrorCode.LIMIT_EXCEEDED,
                        "BASIC plan limit: " + PlanConfig.BASIC_AI_SESSION_LIMIT + " AI coaching sessions/month. Upgrade to FULL for unlimited.");
            }
        }
        // FULL and ANNUAL: no limits

        // 1. Upload audio to Cloudinary
        String audioUrl;
        try {
            audioUrl = mediaService.uploadFile(audioFile, "practice_sessions");
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Failed to upload audio");
        }

        // 2. Call AI Service
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", audioFile.getResource());
        body.add("script_origin", lesson.getContent());
        body.add("target_wpm_min", String.valueOf(lesson.getTargetWpmMin()));
        body.add("target_wpm_max", String.valueOf(lesson.getTargetWpmMax()));
        if (lesson.getEvaluationHint() != null) {
            body.add("evaluation_hint", lesson.getEvaluationHint());
        }
        try {
            body.add("evaluation_criteria_json", objectMapper.writeValueAsString(lesson.getEvaluationCriteria()));
        } catch (Exception e) {
            log.warn("Failed to serialize evaluation criteria, sending empty array", e);
            body.add("evaluation_criteria_json", "[]");
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(AI_SERVICE_URL, requestEntity, Map.class);

            log.info(">>> RAW AI RESPONSE FROM PYTHON: {}", response);

            if (response == null || !"success".equals(response.get("status"))) {
                throw new AppException(ErrorCode.INTERNAL_ERROR, "AI Analysis failed");
            }

            // 3. Save Practice Session with Bilingual Support
            @SuppressWarnings("unchecked")
            List<Map<String, String>> tipsViRaw = (List<Map<String, String>>) response.get("tips_vi");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> tipsEnRaw = (List<Map<String, String>>) response.get("tips_en");

            List<PracticeSession.ExpertTip> expertTipsVi = tipsViRaw.stream()
                    .map(t -> PracticeSession.ExpertTip.builder()
                            .label(t.get("label"))
                            .tip(t.get("tip"))
                            .build())
                    .collect(Collectors.toList());

            List<PracticeSession.ExpertTip> expertTipsEn = tipsEnRaw.stream()
                    .map(t -> PracticeSession.ExpertTip.builder()
                            .label(t.get("label"))
                            .tip(t.get("tip"))
                            .build())
                    .collect(Collectors.toList());

            @SuppressWarnings("unchecked")
            Map<String, Object> criteriaScoresRaw = (Map<String, Object>) response.get("criteria_scores");
            Map<String, Double> criteriaScores = new HashMap<>();
            if (criteriaScoresRaw != null) {
                criteriaScoresRaw.forEach((k, v) -> criteriaScores.put(k, ((Number) v).doubleValue()));
            }
            double overallScore = response.get("overall_score") != null
                    ? ((Number) response.get("overall_score")).doubleValue()
                    : 0.0;

            double cerRate = response.get("cer_rate") != null ? ((Number) response.get("cer_rate")).doubleValue() : 0.0;
            double werRate = response.get("wer_rate") != null ? ((Number) response.get("wer_rate")).doubleValue() : 0.0;

            @SuppressWarnings("unchecked")
            Map<String, Object> spectralFeatures = (Map<String, Object>) response.get("spectral_features");
            @SuppressWarnings("unchecked")
            Map<String, Object> pitchContour = (Map<String, Object>) response.get("pitch_contour");
            @SuppressWarnings("unchecked")
            Map<String, Object> fillerWords = (Map<String, Object>) response.get("filler_words");
            @SuppressWarnings("unchecked")
            Map<String, Object> voiceQuality = (Map<String, Object>) response.get("voice_quality");
            @SuppressWarnings("unchecked")
            Map<String, Object> emotionBreakdown = (Map<String, Object>) response.get("emotion_breakdown");

            PracticeSession session = PracticeSession.builder()
                    .lessonId(lessonId)
                    .userId(userId)
                    .audioUrl(audioUrl)
                    .textSpoken((String) response.get("text_spoken"))
                    .accuracyScore(((Number) response.get("accuracy_score")).doubleValue())
                    .rhythmScore(((Number) response.get("rhythm_score")).doubleValue())
                    .speakingRateWpm(((Number) response.get("speaking_rate_wpm")).doubleValue())
                    .feedbackVi((String) response.get("feedback_vi"))
                    .feedbackEn((String) response.get("feedback_en"))
                    .reportVi((String) response.get("report_vi"))
                    .reportEn((String) response.get("report_en"))
                    .expertTipsVi(expertTipsVi)
                    .expertTipsEn(expertTipsEn)
                    .criteriaScores(criteriaScores)
                    .overallScore(overallScore)
                    .cerRate(cerRate)
                    .werRate(werRate)
                    .spectralFeatures(spectralFeatures)
                    .pitchContour(pitchContour)
                    .fillerWords(fillerWords)
                    .voiceQuality(voiceQuality)
                    .emotionBreakdown(emotionBreakdown)
                    .createdAt(java.time.Instant.now())
                    .build();

            PracticeSession savedSession = sessionRepository.save(session);

            try {
                gamificationService.processPracticeSession(userId, lessonId, session.getAccuracyScore(),
                        session.getRhythmScore());
            } catch (Exception e) {
                log.error("Failed to process gamification stats for user: {}", userId, e);
            }

            // Increment lesson practice count
            lesson.setPracticeCount(lesson.getPracticeCount() + 1);
            lessonRepository.save(lesson);

            // Increment AI session counter — FREE uses total session count via repository,
            // but also track aiSessionsUsed so frontend can display X/5 correctly
            if (plan == SubscriptionPlan.FREE || plan == SubscriptionPlan.DAILY || plan == SubscriptionPlan.BASIC) {
                user.setAiSessionsUsed(user.getAiSessionsUsed() + 1);
                userRepository.save(user);
            }

            // Fire-and-forget adaptive calibration after every session
            adaptiveCalibrationService.calibrateLesson(lessonId);

            PracticeSessionResponseDTO dto = sessionMapper.toResponseDTO(savedSession);
            dto.setLessonTitle(lesson.getTitle());
            return dto;

        } catch (Exception e) {
            log.error("Error calling AI service", e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Error calling AI service: " + e.getMessage());
        }
    }

    @Override
    public List<PracticeSessionResponseDTO> getUserPracticeHistory(String userId) {
        List<PracticeSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Fetch all lesson titles in one go to optimize performance
        List<String> lessonIds = sessions.stream()
                .map(PracticeSession::getLessonId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        log.info(">>> SEARCHING TITLES FOR IDs: {}", lessonIds);

        Map<String, VoiceLesson> lessonMap = lessonRepository.findAllById(lessonIds).stream()
                .collect(Collectors.toMap(VoiceLesson::getId, l -> l, (a, b) -> a));

        log.info(">>> MAP CREATED: {}", lessonMap.keySet());

        return sessions.stream()
                .map(s -> {
                    PracticeSessionResponseDTO dto = sessionMapper.toResponseDTO(s);
                    VoiceLesson lesson = lessonMap.get(s.getLessonId());
                    if (lesson != null) {
                        dto.setLessonTitle(lesson.getTitle());
                        dto.setLessonContent(lesson.getContent());
                        dto.setLessonCategory(lesson.getCategory() != null ? lesson.getCategory().name() : null);
                        dto.setLessonDifficulty(lesson.getDifficulty());
                        dto.setLessonDescription(lesson.getDescription());
                    }
                    log.info(">>> Mapping Session {} -> Lesson {} -> Title: {}", s.getId(), s.getLessonId(),
                            dto.getLessonTitle());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public PracticeSessionResponseDTO getPracticeSessionById(String id) {
        PracticeSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Practice session not found"));

        PracticeSessionResponseDTO dto = sessionMapper.toResponseDTO(session);
        lessonRepository.findById(session.getLessonId())
                .ifPresent(lesson -> dto.setLessonTitle(lesson.getTitle()));

        return dto;
    }

    @Override
    public byte[] generateTTSAudio(String text, String voice) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("text", text);
        body.add("voice", voice != null && !voice.isBlank() ? voice : "F1");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            org.springframework.http.ResponseEntity<byte[]> response = restTemplate.exchange(
                    "http://127.0.0.1:8001/tts/stream",
                    org.springframework.http.HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );
            byte[] wav = response.getBody();
            return wav != null ? wav : new byte[0];
        } catch (Exception e) {
            log.error("Error calling TTS stream service", e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "TTS service error: " + SecurityUtils.safeMessage(e));
        }
    }

    @Override
    public com.mchub.models.LessonAdaptiveStats getAdaptiveStats(String lessonId) {
        return adaptiveStatsRepository.findByLessonId(lessonId).orElse(null);
    }

    @Override
    public List<VoiceLessonResponseDTO> getFeaturedLessons(int limit) {
        return lessonRepository
                .findByPracticeCountGreaterThanOrderByPracticeCountDesc(0, PageRequest.of(0, limit))
                .stream()
                .map(lessonMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Object proxyAnalyzeVoice(MultipartFile audioFile, String scriptOrigin) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", audioFile.getResource());
        body.add("script_origin", scriptOrigin != null ? scriptOrigin : "");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.postForObject(AI_SERVICE_URL, requestEntity, Map.class);
        } catch (Exception e) {
            log.error("AI proxy call failed: {}", e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_ERROR, "AI service unavailable");
        }
    }
}
