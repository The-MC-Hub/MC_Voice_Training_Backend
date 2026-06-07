package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "otp_verifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerification {

    @Id
    private String id;

    @Indexed
    private String email;

    private String code;

    @Indexed(expireAfterSeconds = 600) // TTL: auto-delete after 10 min
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean used = false;

    @CreatedDate
    private LocalDateTime createdAt;
}
