package com.mchub.services;

import com.mchub.dto.PracticeSessionResponseDTO;
import com.mchub.dto.VoiceLessonResponseDTO;
import com.mchub.enums.VoiceLessonCategory;
import com.mchub.models.VoiceLesson;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VoiceService {
    // Admin methods
    VoiceLessonResponseDTO createLesson(String title, String content, VoiceLessonCategory category,
            String difficulty, String description, MultipartFile thumbnail,
            List<VoiceLesson.EvaluationCriteria> evaluationCriteria, String evaluationHint,
            int targetWpmMin, int targetWpmMax, int passingScore);

    VoiceLessonResponseDTO updateLesson(String id, String title, String content, VoiceLessonCategory category,
            String difficulty, String description, MultipartFile thumbnail,
            List<VoiceLesson.EvaluationCriteria> evaluationCriteria, String evaluationHint,
            int targetWpmMin, int targetWpmMax, int passingScore);

    List<VoiceLessonResponseDTO> getAllLessons();
    void deleteLesson(String id);

    // MC methods
    List<VoiceLessonResponseDTO> getLessonsByCategory(VoiceLessonCategory category);
    VoiceLessonResponseDTO getLessonById(String id);
    PracticeSessionResponseDTO analyzePractice(String lessonId, String userId, MultipartFile audioFile);
    List<PracticeSessionResponseDTO> getUserPracticeHistory(String userId);
    PracticeSessionResponseDTO getPracticeSessionById(String id);
}
