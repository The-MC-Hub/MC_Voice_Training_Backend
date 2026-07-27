package com.mchub.models;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "guest_voice_usage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestVoiceUsage {
  @Id private String ipAddress;
  private LocalDateTime lastUsedAt;
}
