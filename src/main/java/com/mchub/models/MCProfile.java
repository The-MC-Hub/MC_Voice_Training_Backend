package com.mchub.models;

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

@Document(collection = "mcprofiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCProfile {

    @Id
    private String id;

    private String user;

    @Builder.Default
    private List<String> regions = new ArrayList<>();

    @Builder.Default
    private int experience = 0;

    @Builder.Default
    private List<String> styles = new ArrayList<>();

    @Builder.Default
    private String biography = "";

    @Builder.Default
    private String personality = "";

    @Builder.Default
    private String hostingStyle = "";

    @Builder.Default
    private List<String> notableEvents = new ArrayList<>();

    @Builder.Default
    private List<String> languages = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
