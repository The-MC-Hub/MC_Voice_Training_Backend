package com.mchub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLogDTO {
    private String id;
    private String campaignId;
    private String email;
    private String status;
    private String errorReason;
    private LocalDateTime sentAt;
}
