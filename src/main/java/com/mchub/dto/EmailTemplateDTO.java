package com.mchub.dto;

import com.mchub.models.EmailTemplate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateDTO {
  private String id;
  private String name;
  private String subject;
  private String htmlContent;
  private EmailTemplate.DesignData designData;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
