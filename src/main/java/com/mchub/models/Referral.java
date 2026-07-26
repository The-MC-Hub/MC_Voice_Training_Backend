package com.mchub.models;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "referrals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Referral {

  @Id private String id;

  private String referrerId;

  private String referredUserId;

  @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
