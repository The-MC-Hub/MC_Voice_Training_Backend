package com.mchub.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {
    private String name;
    private String phoneNumber;
    private String bio;
    private String avatar;
}
