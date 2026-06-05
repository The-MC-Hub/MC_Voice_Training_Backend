package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "system_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLog {

    @Id
    private String id;

    private String level;     // DEBUG, INFO, WARN, ERROR
    private String logger;    // class name (shortened)
    private String message;
    private String source;    // "JAVA" | "AI"
    private String thread;

    @Indexed(expireAfterSeconds = 604800) // TTL: 7 days
    private Instant timestamp;
}
