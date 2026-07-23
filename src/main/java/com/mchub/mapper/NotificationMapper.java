package com.mchub.mapper;

import com.mchub.dto.NotificationResponseDTO;
import com.mchub.models.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {
    NotificationResponseDTO toResponseDTO(Notification notification);
}
