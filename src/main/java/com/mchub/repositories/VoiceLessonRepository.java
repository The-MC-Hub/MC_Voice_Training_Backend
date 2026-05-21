package com.mchub.repositories;

import com.mchub.enums.VoiceLessonCategory;
import com.mchub.models.VoiceLesson;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoiceLessonRepository extends MongoRepository<VoiceLesson, String> {
    List<VoiceLesson> findByCategory(VoiceLessonCategory category);
}
