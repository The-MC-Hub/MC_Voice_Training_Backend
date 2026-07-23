package com.mchub.repositories;

import com.mchub.models.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    void deleteByConversationId(String conversationId);
}
