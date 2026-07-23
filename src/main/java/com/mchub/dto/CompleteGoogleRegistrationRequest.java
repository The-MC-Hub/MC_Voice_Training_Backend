package com.mchub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompleteGoogleRegistrationRequest {

    // Short-lived signed token identifying the pending Google identity — issued by
    // POST /auth/google when the account doesn't exist yet, never a raw Google id token.
    @NotBlank(message = "pendingToken cannot be empty")
    private String pendingToken;

    @NotBlank(message = "role cannot be empty")
    private String role;

    private String referralCode;
}
