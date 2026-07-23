package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Conversation;
import com.mchub.models.Message;
import com.mchub.services.ChatService;
import com.mchub.repositories.MessageRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.BookingRepository;
import com.mchub.mapper.ConversationMapper;
import com.mchub.mapper.MessageMapper;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConversations() {
        String userId = SecurityUtils.getCurrentUserId();
        List<ConversationResponseDTO> list = chatService.getUserConversations(userId).stream()
                .map(this::populateConversation)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(Map.of("conversations", list)));
    }

    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<MessageResponseDTO> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(page, size)).stream()
                .map(messageMapper::toResponseDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(Map.of("messages", messages)));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConversation(@PathVariable String id) {
        Conversation conversation = chatService.getConversationById(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("conversation", populateConversation(conversation))));
    }

    @PatchMapping("/conversations/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String id) {
        chatService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @PostMapping("/messages/{conversationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Message content cannot be empty");
        }
        String userId = SecurityUtils.getCurrentUserId();
        String type = body.getOrDefault("type", "text");
        Message message = chatService.sendMessage(conversationId, userId, content, type);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", messageMapper.toResponseDTO(message))));
    }

    private ConversationResponseDTO populateConversation(Conversation conv) {
        ConversationResponseDTO dto = conversationMapper.toResponseDTO(conv);

        List<Map<String, Object>> participantDtos = new ArrayList<>();
        if (conv.getParticipants() != null) {
            for (String pid : conv.getParticipants()) {
                userRepository.findById(pid).ifPresent(user -> {
                    Map<String, Object> u = new HashMap<>();
                    u.put("_id", user.getId());
                    u.put("name", user.getName());
                    u.put("avatar", user.getAvatar());
                    u.put("role", user.getRole());
                    participantDtos.add(u);
                });
            }
        }
        dto.setParticipantInfo(participantDtos);

        if (conv.getBookingId() != null) {
            bookingRepository.findById(conv.getBookingId()).ifPresent(booking -> {
                Map<String, Object> b = new HashMap<>();
                b.put("_id", booking.getId());
                b.put("eventName", booking.getEventName());
                dto.setBookingInfo(b);
            });
        }

        if (conv.getLastMessage() != null) {
            messageRepository.findById(conv.getLastMessage()).ifPresent(msg -> {
                Map<String, Object> m = new HashMap<>();
                m.put("_id", msg.getId());
                m.put("content", msg.getContent());
                m.put("type", msg.getType());
                m.put("senderId", msg.getSenderId());
                m.put("createdAt", msg.getCreatedAt());
                dto.setLastMessageInfo(m);
            });
        }

        return dto;
    }
}
