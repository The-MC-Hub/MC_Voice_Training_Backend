package com.mchub.mapper;

import com.mchub.dto.ReportResponseDTO;
import com.mchub.models.Report;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReportMapper {
    @Mapping(target = "reporterName", ignore = true)
    @Mapping(target = "reportedName", ignore = true)
    ReportResponseDTO toResponseDTO(Report report);
}
