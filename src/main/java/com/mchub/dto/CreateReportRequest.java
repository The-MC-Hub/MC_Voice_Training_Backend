package com.mchub.dto;

import com.mchub.enums.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateReportRequest {

  @NotBlank(message = "Reported ID cannot be empty")
  private String reportedId;

  @NotNull(message = "Report reason cannot be empty")
  private ReportReason reason;

  @NotBlank(message = "Description cannot be empty")
  private String description;

  private List<String> evidenceUrls;
}
