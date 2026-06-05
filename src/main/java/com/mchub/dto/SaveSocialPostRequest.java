package com.mchub.dto;

import lombok.Data;

@Data
public class SaveSocialPostRequest {
    private String image;
    private String description;
    private String fbLink;
    private int sortOrder;
    private boolean active = true;
}
