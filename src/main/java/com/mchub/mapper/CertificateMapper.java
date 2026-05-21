package com.mchub.mapper;

import com.mchub.dto.CertificateResponseDTO;
import com.mchub.models.Certificate;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CertificateMapper {
    CertificateResponseDTO toResponseDTO(Certificate certificate);
}
