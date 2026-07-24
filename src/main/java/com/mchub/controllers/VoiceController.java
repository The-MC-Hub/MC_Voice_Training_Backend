package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.PracticeSessionResponseDTO;
import com.mchub.dto.VoiceLessonResponseDTO;
import com.mchub.enums.VoiceLessonCategory;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.LessonAdaptiveStats;
import com.mchub.models.VoiceLesson;
import com.mchub.models.GuestVoiceUsage;
import com.mchub.models.SystemSetting;
import com.mchub.repositories.GuestVoiceUsageRepository;
import com.mchub.repositories.SystemSettingRepository;
import com.mchub.services.VoiceService;
import com.mchub.util.AudioMagicBytesValidator;
import com.mchub.util.ClientIpResolver;
import com.mchub.util.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceController {

    private final VoiceService voiceService;
    private final GuestVoiceUsageRepository guestUsageRepo;
    private final SystemSettingRepository systemSettingRepo;
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

    @PutMapping("/admin/lessons/{id}/sample-audio")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<VoiceLessonResponseDTO>> setSampleAudio(
            @PathVariable String id,
            @RequestParam(required = false) MultipartFile audio) {
        VoiceLessonResponseDTO dto = voiceService.setSampleAudio(id, audio);
        return ResponseEntity.ok(ApiResponse.success(
                audio == null || audio.isEmpty() ? "Sample audio cleared" : "Sample audio uploaded", dto));
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

    private static final List<String> ALLOWED_AUDIO_TYPES = List.of(
            "audio/wav", "audio/mpeg", "audio/webm", "audio/ogg", "audio/mp4", "audio/x-m4a", "audio/x-wav");

    private void validateAudioFile(MultipartFile audioFile) {
        String ct = audioFile.getContentType();
        // Strip codec suffix e.g. "audio/webm;codecs=opus" → "audio/webm"
        String ctBase = ct != null ? ct.split(";")[0].trim().toLowerCase() : "";
        if (ctBase.isEmpty() || !ALLOWED_AUDIO_TYPES.contains(ctBase)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Chỉ hỗ trợ file audio (wav, mp3, webm, ogg, m4a)");
        }
        if (audioFile.getSize() > 20L * 1024 * 1024) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "File không được vượt quá 20MB");
        }
        try {
            if (!AudioMagicBytesValidator.isValidAudio(audioFile.getInputStream())) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Nội dung file không hợp lệ");
            }
        } catch (IOException e) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Không thể đọc file");
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
    @PreAuthorize("hasAuthority('MC')")
    public ResponseEntity<ApiResponse<PracticeSessionResponseDTO>> analyzePractice(
            @RequestParam String lessonId,
            @RequestParam MultipartFile audioFile) {

        validateAudioFile(audioFile);

        String userId = SecurityUtils.getCurrentUserId();
        PracticeSessionResponseDTO dto = voiceService.analyzePractice(lessonId, userId, audioFile);
        return ResponseEntity.ok(ApiResponse.success("Analysis completed", dto));
    }

    @GetMapping("/practice/history/{userId}")
    @PreAuthorize("hasAuthority('MC') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<PracticeSessionResponseDTO>>> getHistory(@PathVariable String userId) {
        String callerId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isAdmin && !callerId.equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Access denied");
        }
        return ResponseEntity.ok(ApiResponse.success(voiceService.getUserPracticeHistory(userId)));
    }

    /** GET /api/v1/voice/practice/history/{userId}/lesson/{lessonId} — sessions for one lesson, oldest first */
    @GetMapping("/practice/history/{userId}/lesson/{lessonId}")
    @PreAuthorize("hasAuthority('MC') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<PracticeSessionResponseDTO>>> getLessonHistory(
            @PathVariable String userId, @PathVariable String lessonId) {
        String callerId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isAdmin && !callerId.equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Access denied");
        }
        return ResponseEntity.ok(ApiResponse.success(voiceService.getUserLessonHistory(userId, lessonId)));
    }

    @PostMapping("/proxy/analyze-voice")
    @PreAuthorize("hasAuthority('MC')")
    public ResponseEntity<?> proxyAnalyzeVoice(
            @RequestParam MultipartFile audioFile,
            @RequestParam(required = false) String scriptOrigin) {

        validateAudioFile(audioFile);

        return ResponseEntity.ok(voiceService.proxyAnalyzeVoice(audioFile, scriptOrigin));
    }

    @PostMapping("/practice/analyze-guest")
    public ResponseEntity<?> analyzeGuestVoice(
            @RequestParam MultipartFile audioFile,
            @RequestParam(required = false) String scriptOrigin,
            HttpServletRequest request) {

        String ip = ClientIpResolver.resolve(request);
        GuestVoiceUsage usage = guestUsageRepo.findById(ip).orElse(null);
        
        int cooldownHours = 3;
        SystemSetting setting = systemSettingRepo.findById("GUEST_COOLDOWN_HOURS").orElse(null);
        if (setting != null) {
            try { cooldownHours = Integer.parseInt(setting.getValue()); } catch (Exception ignored) {}
        }
        
        if (usage != null && usage.getLastUsedAt().plusHours(cooldownHours).isAfter(java.time.LocalDateTime.now())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Bạn đã sử dụng lượt thử miễn phí. Vui lòng quay lại sau " + cooldownHours + " tiếng hoặc đăng ký tài khoản để tiếp tục.");
        }

        validateAudioFile(audioFile);

        var result = voiceService.proxyAnalyzeVoice(audioFile, scriptOrigin);

        if (usage == null) {
            usage = GuestVoiceUsage.builder().ipAddress(ip).build();
        }
        usage.setLastUsedAt(java.time.LocalDateTime.now());
        guestUsageRepo.save(usage);

        return ResponseEntity.ok(ApiResponse.success("Phân tích thành công", result));
    }

    @GetMapping("/practice/{id}")
    @PreAuthorize("hasAuthority('MC') or hasAuthority('ADMIN')")
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
    @PreAuthorize("hasAuthority('MC') or hasAuthority('ADMIN')")
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

    @GetMapping("/guest-cooldown-hours")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGuestCooldownHours() {
        int hours = systemSettingRepo.findById("GUEST_COOLDOWN_HOURS")
                .map(s -> { try { return Integer.parseInt(s.getValue()); } catch (Exception e) { return 3; } })
                .orElse(3);
        return ResponseEntity.ok(ApiResponse.success("OK", Map.of("hours", hours)));
    }
}
