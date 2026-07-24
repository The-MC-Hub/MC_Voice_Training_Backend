package com.mchub.mapper;

import com.mchub.dto.BookingDetailResponseDTO;
import com.mchub.models.BookingDetail;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BookingDetailMapper {

    BookingDetailResponseDTO toResponseDTO(BookingDetail detail);
}
