package com.mchub.mapper;

import com.mchub.dto.ScheduleResponseDTO;
import com.mchub.models.Schedule;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ScheduleMapper {

    ScheduleResponseDTO toResponseDTO(Schedule schedule);
}
