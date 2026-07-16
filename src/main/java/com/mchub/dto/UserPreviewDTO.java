package com.mchub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreviewDTO {
    private String id;
    private String name;
    private String email;
    private String plan;
    private String role;
    private boolean isPremium;
}
