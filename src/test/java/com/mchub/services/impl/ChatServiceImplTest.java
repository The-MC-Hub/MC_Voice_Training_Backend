package com.mchub.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Conversation;
import com.mchub.models.Message;
import com.mchub.repositories.ConversationRepository;
import com.mchub.repositories.MessageRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Unit tests for ChatServiceImpl. Covers the participant-membership guard added to sendMessage
 * (prevents a user from posting into a conversation they are not part of) and the existing
 * locked-conversation / broadcast behavior.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

  @Mock private ConversationRepository conversationRepository;
  @Mock private MessageRepository messageRepository;
  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private ChatServiceImpl chatService;

  private Conversation conversation;

  @BeforeEach
  void setUp() {
    conversation =
        Conversation.builder()
            .id("conv_1")
            .participants(List.of("client_1", "mc_1"))
            .isActive(true)
            .build();
  }

  @Nested
  @DisplayName("sendMessage")
  class SendMessage {

    @Test
    @DisplayName("saves and broadcasts when sender is a participant")
    void sendMessage_participantSender_success() {
      when(conversationRepository.findById("conv_1")).thenReturn(Optional.of(conversation));
      when(messageRepository.save(any(Message.class)))
          .thenAnswer(
              invocation -> {
                Message m = invocation.getArgument(0);
                m.setId("msg_1");
                return m;
              });

      Message result = chatService.sendMessage("conv_1", "client_1", "Hello", "text");

      assertThat(result.getId()).isEqualTo("msg_1");
      assertThat(result.getSenderId()).isEqualTo("client_1");
      verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(Message.class));
    }

    @Test
    @DisplayName("rejects sender who is not a participant of the conversation")
    void sendMessage_nonParticipantSender_throwsAccessDenied() {
      when(conversationRepository.findById("conv_1")).thenReturn(Optional.of(conversation));

      assertThatThrownBy(
              () -> chatService.sendMessage("conv_1", "intruder_99", "Hello", "text"))
          .isInstanceOf(AppException.class)
          .satisfies(
              ex -> assertThat(((AppException) ex).getErrorCode())
                  .isEqualTo(ErrorCode.CONVERSATION_ACCESS_DENIED));

      verify(messageRepository, never()).save(any());
      verify(messagingTemplate, never()).convertAndSend(anyString(), any(Message.class));
    }

    @Test
    @DisplayName("rejects message when conversation is locked, even for a participant")
    void sendMessage_lockedConversation_throwsValidationFailed() {
      conversation.setActive(false);
      when(conversationRepository.findById("conv_1")).thenReturn(Optional.of(conversation));

      assertThatThrownBy(() -> chatService.sendMessage("conv_1", "client_1", "Hello", "text"))
          .isInstanceOf(AppException.class)
          .satisfies(
              ex -> assertThat(((AppException) ex).getErrorCode())
                  .isEqualTo(ErrorCode.VALIDATION_FAILED));

      verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws RESOURCE_NOT_FOUND when conversation does not exist")
    void sendMessage_missingConversation_throwsNotFound() {
      when(conversationRepository.findById("missing")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> chatService.sendMessage("missing", "client_1", "Hello", "text"))
          .isInstanceOf(AppException.class)
          .satisfies(
              ex -> assertThat(((AppException) ex).getErrorCode())
                  .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
  }
}
