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
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.VoiceLessonRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.models.User;
import com.mchub.services.MediaService;
import com.mchub.services.VoiceService;
import com.mchub.services.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final RestTemplate restTemplate;
    private final GamificationService gamificationService;
    private final ObjectMapper objectMapper;

    private static final String AI_SERVICE_URL = "http://127.0.0.1:8001/analyze-voice";

    @Override
    public VoiceLessonResponseDTO createLesson(String title, String content, VoiceLessonCategory category,
            String difficulty, String description, MultipartFile thumbnail,
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
                .evaluationCriteria(evaluationCriteria != null ? evaluationCriteria : new java.util.ArrayList<>())
                .evaluationHint(evaluationHint)
                .targetWpmMin(targetWpmMin)
                .targetWpmMax(targetWpmMax)
                .passingScore(passingScore)
                .build();

        return lessonMapper.toResponseDTO(lessonRepository.save(lesson));
    }

    @Override
    public VoiceLessonResponseDTO updateLesson(String id, String title, String content, VoiceLessonCategory category,
            String difficulty, String description, MultipartFile thumbnail,
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
        return lessonRepository.findAll().stream()
                .map(lessonMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteLesson(String id) {
        lessonRepository.deleteById(id);
    }

    @Override
    public List<VoiceLessonResponseDTO> getLessonsByCategory(VoiceLessonCategory category) {
        return lessonRepository.findByCategory(category).stream()
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

        if (!user.isPremium()) {
            long count = sessionRepository.countByUserId(userId);
            if (count >= 5) {
                throw new AppException(ErrorCode.LIMIT_EXCEEDED, "You have reached the free limit of 5 practice sessions. Please upgrade to Premium to continue practicing.");
            }
        }

        VoiceLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Lesson not found"));

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
                    .createdAt(java.time.Instant.now())
                    .build();

            PracticeSession savedSession = sessionRepository.save(session);
            
            try {
                gamificationService.processPracticeSession(userId, lessonId, session.getAccuracyScore(), session.getRhythmScore());
            } catch (Exception e) {
                log.error("Failed to process gamification stats for user: {}", userId, e);
            }

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
        
        Map<String, String> lessonMap = lessonRepository.findAllById(lessonIds).stream()
                .collect(Collectors.toMap(VoiceLesson::getId, VoiceLesson::getTitle, (a, b) -> a));
        
        log.info(">>> MAP CREATED: {}", lessonMap);

        return sessions.stream()
                .map(s -> {
                    PracticeSessionResponseDTO dto = sessionMapper.toResponseDTO(s);
                    String title = lessonMap.get(s.getLessonId());
                    log.info(">>> Mapping Session {} -> Lesson {} -> Title: {}", s.getId(), s.getLessonId(), title);
                    dto.setLessonTitle(title);
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
}
