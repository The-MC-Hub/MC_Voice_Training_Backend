package com.mchub.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCertificateRequest {

  @NotBlank(message = "Certificate name cannot be empty")
  private String name;

  @NotBlank(message = "Issuer cannot be empty")
  private String issuer;

  private LocalDate issuedDate;
  private LocalDate expiredDate;
  private String imageUrl;
}
