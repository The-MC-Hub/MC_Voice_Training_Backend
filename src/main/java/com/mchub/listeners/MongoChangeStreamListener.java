package com.mchub.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest;
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.Subscription;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mchub.change-stream.enabled", havingValue = "true", matchIfMissing = false)
public class MongoChangeStreamListener {

  private static final Logger log = LoggerFactory.getLogger(MongoChangeStreamListener.class);

  public MongoChangeStreamListener(
      MongoTemplate mongoTemplate, MessageListenerContainer container) {
    
    ChangeStreamRequest<Object> request =
        ChangeStreamRequest.builder()
            .collection("voice_lessons")
            .publishTo(
                message -> {
                  log.info(
                      "MongoDB Change Stream Event Detected: Operation={}, DocumentID={}",
                      message.getOperationType(),
                      message.getRaw().getDocumentKey());
                })
            .build();

    Subscription subscription = container.register(request, Object.class);
    log.info("MongoDB Change Stream Listener initialized for collection: voice_lessons");
  }
}
