package com.mchub.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ConversationResponseDTO {
    private String id;
    private List<String> participants;
    private String bookingId;
    private String lastMessage;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Enriched by controller — not mapped by MapStruct
    private List<Map<String, Object>> participantInfo;
    private Map<String, Object> bookingInfo;
    private Map<String, Object> lastMessageInfo;
}
