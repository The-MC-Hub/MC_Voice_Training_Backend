package com.mchub.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailLogDTO {
    private String id;
    private String campaignId;
    private String email;
    private String status;
    private String errorReason;
    private LocalDateTime sentAt;
}
