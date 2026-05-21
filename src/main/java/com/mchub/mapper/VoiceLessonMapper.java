package com.mchub.mapper;

import com.mchub.dto.VoiceLessonResponseDTO;
import com.mchub.models.VoiceLesson;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VoiceLessonMapper {
    VoiceLessonResponseDTO toResponseDTO(VoiceLesson lesson);
}
