package com.mchub.models;

import com.mchub.enums.VoiceLessonCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;

@Document(indexName = "voice_lessons")
@Setting(settingPath = "/elasticsearch/settings.json")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceLessonSearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "vn_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "vn_analyzer")
    private String description;

    @Field(type = FieldType.Text, analyzer = "vn_analyzer")
    private String content;

    @Field(type = FieldType.Keyword)
    private VoiceLessonCategory category;

    @Field(type = FieldType.Keyword)
    private String difficulty;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;
}