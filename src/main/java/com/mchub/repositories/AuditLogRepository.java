package com.mchub.repositories;

import com.mchub.enums.AuditAction;
import com.mchub.models.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findByUserId(String userId);
    List<AuditLog> findByAction(AuditAction action);
    List<AuditLog> findByResource(String resource);
    List<AuditLog> findByResourceId(String resourceId);
    List<AuditLog> findByStatus(String status);
    List<AuditLog> findByActionAndCreatedAtAfter(AuditAction action, LocalDateTime after);
    List<AuditLog> findByCreatedAtAfter(LocalDateTime after);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByAction(AuditAction action);
    long countByActionAndCreatedAtAfter(AuditAction action, LocalDateTime after);
}
