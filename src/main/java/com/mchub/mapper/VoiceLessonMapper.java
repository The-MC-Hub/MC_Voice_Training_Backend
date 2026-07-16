package com.mchub.mapper;

import com.mchub.dto.VoiceLessonResponseDTO;
import com.mchub.models.VoiceLesson;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VoiceLessonMapper {
    VoiceLessonResponseDTO toResponseDTO(VoiceLesson lesson);
}
