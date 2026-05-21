package com.mchub.mapper;

import com.mchub.dto.PracticeSessionResponseDTO;
import com.mchub.models.PracticeSession;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PracticeSessionMapper {
    PracticeSessionResponseDTO toResponseDTO(PracticeSession session);

    // Explicitly map inner class
    PracticeSessionResponseDTO.ExpertTipDTO toExpertTipDTO(PracticeSession.ExpertTip tip);
}
