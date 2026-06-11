package com.mchub.services;

import com.mchub.dto.PracticeSessionResponseDTO;
import com.mchub.dto.VoiceLessonResponseDTO;
import com.mchub.enums.VoiceLessonCategory;
import com.mchub.models.LessonAdaptiveStats;
import com.mchub.models.VoiceLesson;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VoiceService {
        // Admin methods
        VoiceLessonResponseDTO createLesson(String title, String content, VoiceLessonCategory category,
                        String difficulty, String description, MultipartFile thumbnail, String videoUrl,
                        List<VoiceLesson.EvaluationCriteria> evaluationCriteria, String evaluationHint,
                        int targetWpmMin, int targetWpmMax, int passingScore);

        VoiceLessonResponseDTO updateLesson(String id, String title, String content, VoiceLessonCategory category,
                        String difficulty, String description, MultipartFile thumbnail, String videoUrl,
                        List<VoiceLesson.EvaluationCriteria> evaluationCriteria, String evaluationHint,
                        int targetWpmMin, int targetWpmMax, int passingScore);

        List<VoiceLessonResponseDTO> getAllLessons();

        void deleteLesson(String id);

        // MC methods
        List<VoiceLessonResponseDTO> searchLessons(String searchTerm, VoiceLessonCategory category);

        List<VoiceLessonResponseDTO> getLessonsByCategory(VoiceLessonCategory category);

        VoiceLessonResponseDTO getLessonById(String id);

        PracticeSessionResponseDTO analyzePractice(String lessonId, String userId, MultipartFile audioFile);

        Object proxyAnalyzeVoice(MultipartFile audioFile, String scriptOrigin);

        List<PracticeSessionResponseDTO> getUserPracticeHistory(String userId);

        PracticeSessionResponseDTO getPracticeSessionById(String id);

        /** Calls Python AI /generate-mc-voice and returns raw WAV bytes */
        byte[] generateTTSAudio(String text, String voice);

        /** Returns adaptive calibration stats for a lesson, or null if < 10 sessions */
        LessonAdaptiveStats getAdaptiveStats(String lessonId);

        /** Returns top N lessons ordered by practiceCount desc (only those with > 0 practices) */
        List<VoiceLessonResponseDTO> getFeaturedLessons(int limit);
}
