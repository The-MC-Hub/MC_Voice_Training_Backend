package com.mchub.models;

import com.mchub.enums.EventType;
import com.mchub.enums.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "client_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfile {

    @Id
    private String id;

    private String user;

    @Builder.Default
    private Region region = null;

    @Builder.Default
    private String customRegion = "";

    @Builder.Default
    private List<EventType> preferredEventTypes = new ArrayList<>();

    @Builder.Default
    private String organization = "";

    @Builder.Default
    private String bio = "";

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
