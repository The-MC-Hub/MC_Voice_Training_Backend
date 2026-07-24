package com.mchub.dto;

import com.mchub.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageResponseDTO {
    private String id;
    private String conversationId;
    private String senderId;
    private String content;
    private MessageType type;
    private String bookingId;
    private String attachmentUrl;
    private List<String> readBy;
    private LocalDateTime createdAt;
}
