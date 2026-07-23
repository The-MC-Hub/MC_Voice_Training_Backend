package com.mchub.mapper;

import com.mchub.dto.ReviewResponseDTO;
import com.mchub.models.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReviewMapper {

    @Mapping(target = "bookingId", source = "review.booking")
    @Mapping(target = "clientName", ignore = true)
    @Mapping(target = "clientAvatar", ignore = true)
    ReviewResponseDTO toResponseDTO(Review review);
}
