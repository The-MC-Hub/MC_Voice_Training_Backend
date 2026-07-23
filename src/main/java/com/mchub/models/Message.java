package com.mchub.models;

import com.mchub.enums.MessageType;
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

@Document(collection = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    private String senderId;

    private String content;

    @Builder.Default
    private MessageType type = MessageType.TEXT;

    private String bookingId;

    private String attachmentUrl;

    @Builder.Default
    private List<String> readBy = new ArrayList<>();

    @Indexed
    @CreatedDate
    private LocalDateTime createdAt;
}
