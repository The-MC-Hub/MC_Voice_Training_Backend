package com.mchub.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mchub.mapper.ConversationMapper;
import com.mchub.mapper.MessageMapper;
import com.mchub.models.Conversation;
import com.mchub.repositories.BookingRepository;
import com.mchub.repositories.MessageRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.ChatService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for ChatController. Covers the participant-membership guard (assertParticipant) shared by
 * getMessages/getConversation/markAsRead, and the typing-broadcast STOMP handler.
 */
@WebMvcTest(controllers = ChatController.class)
@ContextConfiguration(classes = {ChatController.class, com.mchub.exception.GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ChatService chatService;
  @MockBean private MessageRepository messageRepository;
  @MockBean private UserRepository userRepository;
  @MockBean private BookingRepository bookingRepository;
  @MockBean private ConversationMapper conversationMapper;
  @MockBean private MessageMapper messageMapper;
  @MockBean private SimpMessagingTemplate messagingTemplate;

  @Autowired private ChatController chatController;

  private Conversation conversationOf(String... participants) {
    return Conversation.builder().id("conv_1").participants(List.of(participants)).isActive(true).build();
  }

  @BeforeEach
  void setUp() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "client_1", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  @Nested
  @DisplayName("assertParticipant guard")
  class ParticipantGuard {

    @Test
    @DisplayName("getMessages returns messages for a participant")
    void getMessages_participant_success() throws Exception {
      when(chatService.getConversationById("conv_1"))
          .thenReturn(conversationOf("client_1", "mc_1"));
      when(messageRepository.findByConversationIdOrderByCreatedAtDesc(eq("conv_1"), any()))
          .thenReturn(List.of());

      mockMvc
          .perform(get("/api/v1/chat/messages/conv_1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("getMessages rejects a non-participant")
    void getMessages_nonParticipant_forbidden() throws Exception {
      when(chatService.getConversationById("conv_1"))
          .thenReturn(conversationOf("mc_1", "mc_2"));

      mockMvc
          .perform(get("/api/v1/chat/messages/conv_1"))
          .andExpect(status().isForbidden());

      verify(messageRepository, never()).findByConversationIdOrderByCreatedAtDesc(anyString(), any());
    }

    @Test
    @DisplayName("markAsRead rejects a non-participant")
    void markAsRead_nonParticipant_forbidden() throws Exception {
      when(chatService.getConversationById("conv_1"))
          .thenReturn(conversationOf("mc_1", "mc_2"));

      mockMvc
          .perform(patch("/api/v1/chat/conversations/conv_1/read"))
          .andExpect(status().isForbidden());

      verify(chatService, never()).markAsRead(anyString());
    }
  }

  @Nested
  @DisplayName("handleTyping (STOMP)")
  class HandleTyping {

    @Test
    @DisplayName("broadcasts typing event to other participants only")
    void handleTyping_participant_broadcastsToOthers() {
      when(chatService.getConversationById("conv_1"))
          .thenReturn(conversationOf("client_1", "mc_1"));

      chatController.handleTyping("conv_1", Map.of("userId", "client_1", "isTyping", true));

      verify(messagingTemplate, times(1))
          .convertAndSend(eq("/topic/chat/typing/mc_1"), any(Object.class));
      verify(messagingTemplate, never())
          .convertAndSend(eq("/topic/chat/typing/client_1"), any(Object.class));
    }

    @Test
    @DisplayName("ignores typing event from a non-participant")
    void handleTyping_nonParticipant_noBroadcast() {
      when(chatService.getConversationById("conv_1"))
          .thenReturn(conversationOf("mc_1", "mc_2"));

      chatController.handleTyping("conv_1", Map.of("userId", "intruder_99", "isTyping", true));

      verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
  }
}
