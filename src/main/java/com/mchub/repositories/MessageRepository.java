package com.mchub.repositories;

import com.mchub.models.Message;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

  List<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

  void deleteByConversationId(String conversationId);
}
