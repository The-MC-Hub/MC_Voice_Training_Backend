package com.mchub.repositories;

import com.mchub.models.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    
    List<Notification> findByUserOrderByCreatedAtDesc(String userId, Pageable pageable);

    
    long countByUserAndIsReadFalse(String userId);

    
    void deleteByUser(String userId);
}
