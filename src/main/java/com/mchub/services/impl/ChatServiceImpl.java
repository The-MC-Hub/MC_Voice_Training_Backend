package com.mchub.services.impl;

import com.mchub.enums.MessageType;
import com.mchub.models.Conversation;
import com.mchub.models.Message;
import com.mchub.repositories.ConversationRepository;
import com.mchub.repositories.MessageRepository;
import com.mchub.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Message sendMessage(@NonNull String conversationId, @NonNull String senderId, @NonNull String content, String type) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.isActive()) {
            throw new RuntimeException("Conversation is locked");
        }

        MessageType msgType;
        try {
            msgType = (type != null) ? MessageType.valueOf(type.toUpperCase()) : MessageType.TEXT;
        } catch (IllegalArgumentException e) {
            msgType = MessageType.TEXT;
        }

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content)
                .type(msgType)
                .createdAt(LocalDateTime.now())
                .build();

        Message savedMessage = messageRepository.save(message);
        updateLastMessageAsync(conversationId, savedMessage.getId());
        broadcastMessage(conversation, savedMessage);
        return savedMessage;
    }

    @Override
    @Async
    public void updateLastMessageAsync(@NonNull String conversationId, @NonNull String messageId) {
        conversationRepository.findById(conversationId).ifPresent(c -> {
            c.setLastMessage(messageId);
            c.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(c);
        });
    }

    @Override
    public Conversation createConversationForBooking(@NonNull String bookingId, @NonNull String clientId, @NonNull String mcId) {
        return conversationRepository.findExisting(clientId, mcId, bookingId)
                .orElseGet(() -> {
                    Conversation conv = Conversation.builder()
                            .participants(List.of(clientId, mcId))
                            .bookingId(bookingId)
                            .isActive(true)
                            .build();
                    return conversationRepository.save(conv);
                });
    }

    @Override
    public List<Conversation> getUserConversations(@NonNull String userId) {
        return conversationRepository.findByParticipantsContaining(userId);
    }

    @Override
    public Conversation getConversationById(@NonNull String id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
    }

    @Override
    public void markAsRead(@NonNull String id) {
        conversationRepository.findById(id).ifPresent(c -> {
            c.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(c);
        });
    }

    @Override
    public void deactivateConversationByBookingId(@NonNull String bookingId) {
        conversationRepository.findByBookingId(bookingId).ifPresent(c -> {
            c.setActive(false);
            conversationRepository.save(c);
        });
    }

    private void broadcastMessage(@NonNull Conversation conversation, @NonNull Message message) {
        for (String participantId : conversation.getParticipants()) {
            messagingTemplate.convertAndSend("/topic/chat/" + participantId, message);
        }
    }
}
