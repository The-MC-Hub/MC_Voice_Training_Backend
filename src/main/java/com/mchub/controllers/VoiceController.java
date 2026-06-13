package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.PracticeSessionResponseDTO;
import com.mchub.dto.VoiceLessonResponseDTO;
import com.mchub.enums.VoiceLessonCategory;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.LessonAdaptiveStats;
import com.mchub.models.VoiceLesson;
import com.mchub.services.VoiceService;
import com.mchub.util.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceController {

    private final VoiceService voiceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- Admin Endpoints ---
    @PostMapping("/admin/lessons")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<VoiceLessonResponseDTO>> createLesson(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam VoiceLessonCategory category,
            @RequestParam String difficulty,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile thumbnail,
            @RequestParam(required = false) String videoUrl,
            @RequestParam(required = false) String evaluationHint,
            @RequestParam(required = false) String evaluationCriteriaJson,
            @RequestParam(defaultValue = "120") int targetWpmMin,
            @RequestParam(defaultValue = "150") int targetWpmMax,
            @RequestParam(defaultValue = "70") int passingScore) {

        List<VoiceLesson.EvaluationCriteria> criteria = parseCriteria(evaluationCriteriaJson);
        VoiceLessonResponseDTO dto = voiceService.createLesson(
                title, content, category, difficulty, description, thumbnail, videoUrl,
                criteria, evaluationHint, targetWpmMin, targetWpmMax, passingScore);
        return ResponseEntity.ok(ApiResponse.success("Lesson created successfully", dto));
    }

    @PutMapping("/admin/lessons/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<VoiceLessonResponseDTO>> updateLesson(
            @PathVariable String id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam VoiceLessonCategory category,
            @RequestParam String difficulty,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile thumbnail,
            @RequestParam(required = false) String videoUrl,
            @RequestParam(required = false) String evaluationHint,
            @RequestParam(required = false) String evaluationCriteriaJson,
            @RequestParam(defaultValue = "120") int targetWpmMin,
            @RequestParam(defaultValue = "150") int targetWpmMax,
            @RequestParam(defaultValue = "70") int passingScore) {

        List<VoiceLesson.EvaluationCriteria> criteria = parseCriteria(evaluationCriteriaJson);
        VoiceLessonResponseDTO dto = voiceService.updateLesson(
                id, title, content, category, difficulty, description, thumbnail, videoUrl,
                criteria, evaluationHint, targetWpmMin, targetWpmMax, passingScore);
        return ResponseEntity.ok(ApiResponse.success("Lesson updated successfully", dto));
    }

    private List<VoiceLesson.EvaluationCriteria> parseCriteria(String json) {
        if (json == null || json.isBlank())
            return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<VoiceLesson.EvaluationCriteria>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse evaluationCriteriaJson: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @DeleteMapping("/admin/lessons/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(@PathVariable String id) {
        voiceService.deleteLesson(id);
        return ResponseEntity.ok(ApiResponse.success("Lesson deleted successfully", null));
    }

    // --- Public/MC Endpoints ---
    @GetMapping("/lessons")
    public ResponseEntity<ApiResponse<List<VoiceLessonResponseDTO>>> getAllLessons(
            @RequestParam(required = false) VoiceLessonCategory category,
            @RequestParam(required = false) String search) {

        List<VoiceLessonResponseDTO> lessons = search != null && !search.isBlank()
                ? voiceService.searchLessons(search, category)
                : (category != null ? voiceService.getLessonsByCategory(category) : voiceService.getAllLessons());
        return ResponseEntity.ok(ApiResponse.success(lessons));
    }

    @GetMapping("/lessons/{id}")
    public ResponseEntity<ApiResponse<VoiceLessonResponseDTO>> getLessonById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(voiceService.getLessonById(id)));
    }

    @GetMapping("/lessons/featured")
    public ResponseEntity<ApiResponse<List<VoiceLessonResponseDTO>>> getFeaturedLessons(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.success(voiceService.getFeaturedLessons(limit)));
    }

    // --- MC Practice Endpoints ---
    @PostMapping("/practice/analyze-voice")
    @PreAuthorize("hasAuthority('MC') or hasAuthority('CLIENT')")
    public ResponseEntity<ApiResponse<PracticeSessionResponseDTO>> analyzePractice(
            @RequestParam String lessonId,
            @RequestParam MultipartFile audioFile) {

        String ct = audioFile.getContentType();
        // Strip codec suffix e.g. "audio/webm;codecs=opus" → "audio/webm"
        String ctBase = ct != null ? ct.split(";")[0].trim().toLowerCase() : "";
        List<String> allowedTypes = List.of("audio/wav", "audio/mpeg", "audio/webm", "audio/ogg", "audio/mp4", "audio/x-m4a", "audio/x-wav");
        if (ctBase.isEmpty() || !allowedTypes.contains(ctBase)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Chỉ hỗ trợ file audio (wav, mp3, webm, ogg, m4a)");
        }
        if (audioFile.getSize() > 20L * 1024 * 1024) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "File không được vượt quá 20MB");
        }

        String userId = SecurityUtils.getCurrentUserId();
        PracticeSessionResponseDTO dto = voiceService.analyzePractice(lessonId, userId, audioFile);
        return ResponseEntity.ok(ApiResponse.success("Analysis completed", dto));
    }

    @GetMapping("/practice/history/{userId}")
    @PreAuthorize("hasAuthority('MC') or hasAuthority('ADMIN') or hasAuthority('CLIENT')")
    public ResponseEntity<ApiResponse<List<PracticeSessionResponseDTO>>> getHistory(@PathVariable String userId) {
        String callerId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isAdmin && !callerId.equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Access denied");
        }
        return ResponseEntity.ok(ApiResponse.success(voiceService.getUserPracticeHistory(userId)));
    }

    @PostMapping("/proxy/analyze-voice")
    @PreAuthorize("hasAuthority('MC') or hasAuthority('CLIENT')")
    public ResponseEntity<?> proxyAnalyzeVoice(
            @RequestParam MultipartFile audioFile,
            @RequestParam(required = false) String scriptOrigin) {

        String ct = audioFile.getContentType();
        String ctBase = ct != null ? ct.split(";")[0].trim().toLowerCase() : "";
        List<String> allowedTypes = List.of("audio/wav", "audio/mpeg", "audio/webm", "audio/ogg", "audio/mp4", "audio/x-m4a", "audio/x-wav");
        if (ctBase.isEmpty() || !allowedTypes.contains(ctBase)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Chỉ hỗ trợ file audio (wav, mp3, webm, ogg, m4a)");
        }
        if (audioFile.getSize() > 20L * 1024 * 1024) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "File không được vượt quá 20MB");
        }

        return ResponseEntity.ok(voiceService.proxyAnalyzeVoice(audioFile, scriptOrigin));
    }

    @GetMapping("/practice/{id}")
    @PreAuthorize("hasAuthority('MC') or hasAuthority('ADMIN') or hasAuthority('CLIENT')")
    public ResponseEntity<ApiResponse<PracticeSessionResponseDTO>> getPracticeById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(voiceService.getPracticeSessionById(id)));
    }

    // --- Adaptive Stats Endpoint ---
    @GetMapping("/lessons/{id}/adaptive-stats")
    public ResponseEntity<ApiResponse<LessonAdaptiveStats>> getAdaptiveStats(@PathVariable String id) {
        LessonAdaptiveStats stats = voiceService.getAdaptiveStats(id);
        if (stats == null) {
            return ResponseEntity.ok(ApiResponse.success("No adaptive data yet (need 10+ sessions)", null));
        }
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // --- TTS Endpoint ---
    @PostMapping("/tts/generate")
    @PreAuthorize("hasAuthority('MC') or hasAuthority('CLIENT') or hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> generateTTS(
            @RequestParam String text,
            @RequestParam(required = false, defaultValue = "F1") String voice) {

        byte[] wavBytes = voiceService.generateTTSAudio(text, voice);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"mc_voice.wav\"");
        headers.setContentLength(wavBytes.length);

        return ResponseEntity.ok().headers(headers).body(wavBytes);
    }
}
