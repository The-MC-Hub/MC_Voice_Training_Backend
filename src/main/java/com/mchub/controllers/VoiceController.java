package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.PracticeSessionResponseDTO;
import com.mchub.dto.VoiceLessonResponseDTO;
import com.mchub.enums.VoiceLessonCategory;
import com.mchub.models.VoiceLesson;
import com.mchub.services.VoiceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // --- MC Practice Endpoints ---
    @PostMapping("/practice/analyze-voice")
    @PreAuthorize("hasAuthority('MC') or hasAuthority('CLIENT')")
    public ResponseEntity<ApiResponse<PracticeSessionResponseDTO>> analyzePractice(
            @RequestParam String lessonId,
            @RequestParam String userId,
            @RequestParam MultipartFile audioFile) {

        PracticeSessionResponseDTO dto = voiceService.analyzePractice(lessonId, userId, audioFile);
        return ResponseEntity.ok(ApiResponse.success("Analysis completed", dto));
    }

    @GetMapping("/practice/history/{userId}")
    @PreAuthorize("hasAuthority('MC') or hasAuthority('ADMIN') or hasAuthority('CLIENT')")
    public ResponseEntity<ApiResponse<List<PracticeSessionResponseDTO>>> getHistory(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(voiceService.getUserPracticeHistory(userId)));
    }

    @GetMapping("/practice/{id}")
    @PreAuthorize("hasAuthority('MC') or hasAuthority('ADMIN') or hasAuthority('CLIENT')")
    public ResponseEntity<ApiResponse<PracticeSessionResponseDTO>> getPracticeById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(voiceService.getPracticeSessionById(id)));
    }
}
