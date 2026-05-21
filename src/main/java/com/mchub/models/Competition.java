package com.mchub.models;

import com.mchub.enums.CompetitionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "competitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Competition {
    @Id
    private String id;

    private String title;
    private String description;
    private CompetitionType type;
    private String challengeScriptId; // Associated Script id
    
    private Instant startDate;
    private Instant endDate;
    
    @Builder.Default
    private boolean active = true;
}
