package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "case_studies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseStudy {

    @Id
    private String id;

    private String title;

    private String videoUrl;

    private String transcript;

    @Builder.Default
    private List<Annotation> annotations = new ArrayList<>();

    @Builder.Default
    private List<String> discussionQuestions = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Annotation {
        private int timestampSeconds;
        private String comment;
        private String tag; // STRENGTH | WEAKNESS | TECHNIQUE
    }
}
