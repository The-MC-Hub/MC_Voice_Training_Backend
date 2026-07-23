package com.mchub.models;

import com.mchub.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "search_interests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "client_eventtype", def = "{'clientId': 1, 'eventTypes': 1}")
public class SearchInterest {

    @Id
    private String id;

    @Indexed
    private String clientId;

    private String keyword;

    private List<EventType> eventTypes;

    private List<String> regions;

    private Double budgetMin;
    private Double budgetMax;

    private Integer minExperience;

    @Builder.Default
    private int searchCount = 1;

    @CreatedDate
    private LocalDateTime firstSearchedAt;

    private LocalDateTime lastSearchedAt;

    // Last time a MC_RECOMMENDATION notification was sent for this interest — throttles repeats
    private LocalDateTime lastNotifiedAt;
}
