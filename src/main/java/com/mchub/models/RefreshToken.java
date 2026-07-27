package com.mchub.models;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "refreshtokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

  @Id private String id;

  @Indexed private String userId;

  @Indexed(unique = true)
  private String token;

  private LocalDateTime expiresAt;

  @Builder.Default private boolean isRevoked = false;

  private String deviceInfo;

  private String ipAddress;

  private String userAgent;

  @CreatedDate private LocalDateTime createdAt;
}
