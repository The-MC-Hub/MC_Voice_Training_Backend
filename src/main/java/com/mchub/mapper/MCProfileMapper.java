package com.mchub.mapper;

import com.mchub.dto.MCProfileResponseDTO;
import com.mchub.models.MCProfile;
import com.mchub.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MCProfileMapper {

    @Mapping(target = "id", source = "profile.id")
    @Mapping(target = "userId", source = "profile.user")
    @Mapping(target = "experience", source = "profile.experience")
    @Mapping(target = "styles", source = "profile.styles")
    @Mapping(target = "biography", source = "profile.biography")

    @Mapping(target = "name", source = "user.name", defaultValue = "Unknown MC")
    @Mapping(target = "avatar", source = "user.avatar", defaultValue = "default-avatar.png")
    @Mapping(target = "verified", source = "user.verified")
    MCProfileResponseDTO toResponseDTO(MCProfile profile, User user);
}
