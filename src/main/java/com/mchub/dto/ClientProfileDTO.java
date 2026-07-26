package com.mchub.dto;

import com.mchub.enums.EventType;
import com.mchub.enums.Region;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfileDTO {
  private String id;
  private String userId;
  private Region region;
  private String customRegion;
  private List<EventType> preferredEventTypes;
  private String organization;
  private String bio;
}
