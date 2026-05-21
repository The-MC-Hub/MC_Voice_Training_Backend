package com.mchub.models;

import com.mchub.enums.ReportReason;
import com.mchub.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Document(collection = "reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    private String id;

    
    @Indexed
    private String reporterId;

    
    @Indexed
    private String reportedId;

    

    
    private ReportReason reason;

    
    private String description;

    
    @Builder.Default
    private List<String> evidenceUrls = new ArrayList<>();

    
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    
    private String adminNote;

    
    private String resolvedBy;

    
    private LocalDateTime resolvedAt;

    @Indexed
    @CreatedDate
    private LocalDateTime createdAt;
}
