package com.mchub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "idToken cannot be empty")
    private String idToken;
}
