package com.mchub.mapper;

import com.mchub.dto.ClientProfileDTO;
import com.mchub.models.ClientProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ClientProfileMapper {

    @Mapping(target = "userId", source = "profile.user")
    ClientProfileDTO toDTO(ClientProfile profile);
}
