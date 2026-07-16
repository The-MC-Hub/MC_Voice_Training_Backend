package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "email_logs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailLog {
    @Id private String id;
    @Indexed private String campaignId;
    @Indexed private String email;
    private String status;
    private String errorReason;
    @Builder.Default private LocalDateTime sentAt = LocalDateTime.now();
}
