package com.mchub.repositories;

import com.mchub.models.QuickReply;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuickReplyRepository extends MongoRepository<QuickReply, String> {
  List<QuickReply> findByMcUserIdOrderByDisplayOrderAsc(String mcUserId);
}
