package com.mchub.mapper;

import com.mchub.dto.TransactionResponseDTO;
import com.mchub.models.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BookingTransactionMapper {

    TransactionResponseDTO toResponseDTO(Transaction transaction);
}
