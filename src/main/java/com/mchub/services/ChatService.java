package com.mchub.services;

import com.mchub.models.Conversation;
import com.mchub.models.Message;
import org.springframework.lang.NonNull;

import java.util.List;

public interface ChatService {

    Message sendMessage(@NonNull String conversationId, @NonNull String senderId, @NonNull String content, String type);

    void updateLastMessageAsync(@NonNull String conversationId, @NonNull String messageId);

    Conversation createConversationForBooking(@NonNull String bookingId, @NonNull String clientId, @NonNull String mcId);

    List<Conversation> getUserConversations(@NonNull String userId);

    Conversation getConversationById(@NonNull String id);

    void markAsRead(@NonNull String id);

    void deactivateConversationByBookingId(@NonNull String bookingId);
}
