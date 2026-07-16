package com.mchub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveSocialPostRequest {
    @NotBlank
    private String image;
    private String description;
    @NotBlank
    private String fbLink;
    private int sortOrder;
    private boolean active = true;
}
