package com.mchub.models;

import com.mchub.enums.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "auditlogs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

        @Indexed
    private String userId;

        @Indexed
    private AuditAction action;

        private String resource;

        private String resourceId;

        private String details;

        private String ipAddress;

        private String userAgent;

        @Builder.Default
    private String status = "SUCCESS";

        private String errorMessage;

    @Indexed
    @CreatedDate
    private LocalDateTime createdAt;
}
