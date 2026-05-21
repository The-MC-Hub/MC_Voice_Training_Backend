package com.mchub.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MCProfileResponseDTO {
    private String id;
    private String userId;
    private String name;
    private String email;
    private String avatar;
    private boolean isVerified;
    private int experience;
    private List<String> styles;
    private String biography;
}
