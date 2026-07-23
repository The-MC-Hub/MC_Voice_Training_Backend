package com.mchub.mapper;

import com.mchub.dto.BookingResponseDTO;
import com.mchub.models.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BookingMapper {

    @Mapping(target = "clientName", ignore = true)
    @Mapping(target = "mcName", ignore = true)
    @Mapping(target = "clientAvatar", ignore = true)
    @Mapping(target = "mcAvatar", ignore = true)
    @Mapping(target = "mcRatesMin", ignore = true)
    @Mapping(target = "mcRatesMax", ignore = true)
    @Mapping(target = "mcExperience", ignore = true)
    @Mapping(target = "mcRating", ignore = true)
    @Mapping(target = "mcRegion", ignore = true)
    @Mapping(target = "mcEventTypes", ignore = true)
    BookingResponseDTO toResponseDTO(Booking booking);
}
