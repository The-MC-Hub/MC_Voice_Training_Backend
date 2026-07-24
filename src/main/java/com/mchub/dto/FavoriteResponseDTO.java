package com.mchub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FavoriteResponseDTO {
    private String id;
    private String clientId;
    private String mcUserId;
    private LocalDateTime createdAt;
}
