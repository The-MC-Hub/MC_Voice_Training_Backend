package com.mchub.mapper;

import com.mchub.dto.UserResponseDTO;
import com.mchub.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    @Mapping(target = "name", source = "name", defaultValue = "Unknown")
    @Mapping(target = "email", source = "email", defaultValue = "")
    @Mapping(target = "googleLinked", expression = "java(user.getGoogleId() != null)")
    UserResponseDTO toResponseDTO(User user);
}
