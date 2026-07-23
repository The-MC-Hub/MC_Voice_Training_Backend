package com.mchub.mapper;

import com.mchub.dto.MessageResponseDTO;
import com.mchub.models.Message;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MessageMapper {

    MessageResponseDTO toResponseDTO(Message message);
}
