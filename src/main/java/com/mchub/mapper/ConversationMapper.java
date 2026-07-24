package com.mchub.mapper;

import com.mchub.dto.ConversationResponseDTO;
import com.mchub.models.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ConversationMapper {

    ConversationResponseDTO toResponseDTO(Conversation conversation);
}
