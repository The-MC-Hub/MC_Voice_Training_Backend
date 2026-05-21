package com.mchub.mapper;

import com.mchub.dto.AuditLogResponseDTO;
import com.mchub.models.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuditLogMapper {
    AuditLogResponseDTO toResponseDTO(AuditLog auditLog);
}
