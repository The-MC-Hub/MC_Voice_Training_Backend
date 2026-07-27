package com.mchub.controllers;

import com.mchub.dto.*;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.mapper.ConversationMapper;
import com.mchub.mapper.MessageMapper;
import com.mchub.models.Conversation;
import com.mchub.models.Message;
import com.mchub.repositories.BookingRepository;
import com.mchub.repositories.MessageRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.ChatService;
import com.mchub.util.SecurityUtils;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

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
  private final SimpMessagingTemplate messagingTemplate;

  @GetMapping("/conversations")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getConversations() {
    String userId = SecurityUtils.getCurrentUserId();
    List<ConversationResponseDTO> list =
        chatService.getUserConversations(userId).stream().map(this::populateConversation).toList();
    return ResponseEntity.ok(ApiResponse.success(Map.of("conversations", list)));
  }

  @GetMapping("/messages/{conversationId}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getMessages(
      @PathVariable String conversationId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    assertParticipant(conversationId);
    List<MessageResponseDTO> messages =
        messageRepository
            .findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(page, size))
            .stream()
            .map(messageMapper::toResponseDTO)
            .toList();
    return ResponseEntity.ok(ApiResponse.success(Map.of("messages", messages)));
  }

  @GetMapping("/conversations/{id}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getConversation(@PathVariable String id) {
    Conversation conversation = chatService.getConversationById(id);
    assertParticipant(conversation);
    return ResponseEntity.ok(
        ApiResponse.success(Map.of("conversation", populateConversation(conversation))));
  }

  @PatchMapping("/conversations/{id}/read")
  public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String id) {
    assertParticipant(id);
    chatService.markAsRead(id);
    return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
  }

  private void assertParticipant(String conversationId) {
    assertParticipant(chatService.getConversationById(conversationId));
  }

  private void assertParticipant(Conversation conversation) {
    String userId = SecurityUtils.getCurrentUserId();
    if (conversation.getParticipants() == null || !conversation.getParticipants().contains(userId)) {
      throw new AppException(ErrorCode.CONVERSATION_ACCESS_DENIED);
    }
  }

  @MessageMapping("/chat.typing/{conversationId}")
  public void handleTyping(
      @DestinationVariable String conversationId, Map<String, Object> payload) {
    Conversation conversation = chatService.getConversationById(conversationId);
    String userId = String.valueOf(payload.get("userId"));
    if (conversation.getParticipants() == null || !conversation.getParticipants().contains(userId)) {
      return;
    }
    Map<String, Object> event =
        Map.of("conversationId", conversationId, "userId", userId, "isTyping", payload.getOrDefault("isTyping", false));
    for (String participantId : conversation.getParticipants()) {
      if (!participantId.equals(userId)) {
        messagingTemplate.convertAndSend("/topic/chat/typing/" + participantId, event);
      }
    }
  }

  @PostMapping("/messages/{conversationId}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
      @PathVariable String conversationId, @RequestBody Map<String, String> body) {
    String content = body.get("content");
    if (content == null || content.isBlank()) {
      throw new AppException(ErrorCode.VALIDATION_FAILED, "Message content cannot be empty");
    }
    String userId = SecurityUtils.getCurrentUserId();
    String type = body.getOrDefault("type", "text");
    
    // Safety check for off-platform contact details (phone numbers, bank accounts, external chat apps)
    boolean hasOffPlatformRisk = content.matches(".*(\\b0[3|5|7|8|9][0-9]{8}\\b|\\bzalo\\b|\\btelegram\\b|\\bchuyển khoản\\b|\\bstk\\b).*");
    
    Message message = chatService.sendMessage(conversationId, userId, content, type);
    MessageResponseDTO messageDto = messageMapper.toResponseDTO(message);
    
    Map<String, Object> data = new HashMap<>();
    data.put("message", messageDto);
    if (hasOffPlatformRisk) {
      data.put("safetyWarning", "Lưu ý: Để đảm bảo quyền lợi hợp đồng và hoàn tiền khi xảy ra sự cố, vui lòng thực hiện tất cả giao dịch và trao đổi trên MC Hub.");
    }
    
    return ResponseEntity.ok(ApiResponse.success(data));
  }

  private ConversationResponseDTO populateConversation(Conversation conv) {
    ConversationResponseDTO dto = conversationMapper.toResponseDTO(conv);

    List<Map<String, Object>> participantDtos = new ArrayList<>();
    if (conv.getParticipants() != null) {
      for (String pid : conv.getParticipants()) {
        userRepository
            .findById(pid)
            .ifPresent(
                user -> {
                  Map<String, Object> u = new HashMap<>();
                  u.put("id", user.getId());
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
      bookingRepository
          .findById(conv.getBookingId())
          .ifPresent(
              booking -> {
                Map<String, Object> b = new HashMap<>();
                b.put("id", booking.getId());
                b.put("_id", booking.getId());
                b.put("eventName", booking.getEventName());
                dto.setBookingInfo(b);
              });
    }

    if (conv.getLastMessage() != null) {
      messageRepository
          .findById(conv.getLastMessage())
          .ifPresent(
              msg -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", msg.getId());
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
