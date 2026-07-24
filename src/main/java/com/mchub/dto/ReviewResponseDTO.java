package com.mchub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponseDTO {
    private String id;
    private String bookingId;
    private String mc;
    private String client;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    private String clientName;
    private String clientAvatar;
}
