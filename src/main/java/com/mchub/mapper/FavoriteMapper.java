package com.mchub.mapper;

import com.mchub.dto.FavoriteResponseDTO;
import com.mchub.models.Favorite;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FavoriteMapper {

    FavoriteResponseDTO toResponseDTO(Favorite favorite);
}
