package com.mchub.repositories;

import com.mchub.enums.NotificationType;
import com.mchub.models.Notification;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
  List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

  long countByUserIdAndReadFalse(String userId);

  // Admin stats
  long countByType(NotificationType type);

  long countByRead(boolean read);
}
