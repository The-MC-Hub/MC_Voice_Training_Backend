package com.mchub.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

@ExtendWith(MockitoExtension.class)
class SystemHealthServiceImplTest {

  @Mock private MongoTemplate mongoTemplate;
  @Mock private com.mongodb.client.MongoDatabase mongoDatabase;

  @InjectMocks private SystemHealthServiceImpl systemHealthService;

  @Test
  @DisplayName("getSystemHealth returns memory, thread, system, and DB status metrics")
  void getSystemHealthReturnsMetrics() {
    when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
    when(mongoDatabase.runCommand(any(Document.class))).thenReturn(new Document("ok", 1.0));

    Map<String, Object> health = systemHealthService.getSystemHealth();

    assertThat(health).containsKeys("status", "dbStatus", "memory", "threads", "system");
    assertThat(health.get("status")).isEqualTo("UP");
    assertThat(health.get("dbStatus")).isEqualTo("UP");
  }
}
