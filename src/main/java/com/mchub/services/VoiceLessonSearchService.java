package com.mchub.services;

import com.mchub.enums.VoiceLessonCategory;
import com.mchub.models.VoiceLesson;

import java.util.List;

public interface VoiceLessonSearchService {
    void indexLesson(VoiceLesson lesson);
    void deleteLesson(String id);
    List<VoiceLesson> searchLessons(String searchTerm, VoiceLessonCategory category);
    long reindexAll();
}