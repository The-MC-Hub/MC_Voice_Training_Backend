package com.mchub.repositories;

import com.mchub.models.SystemLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SystemLogRepository extends MongoRepository<SystemLog, String> {

    List<SystemLog> findTop200ByOrderByTimestampDesc();

    List<SystemLog> findByOrderByTimestampDesc(Pageable pageable);

    List<SystemLog> findByLevelOrderByTimestampDesc(String level, Pageable pageable);

    List<SystemLog> findBySourceOrderByTimestampDesc(String source, Pageable pageable);

    List<SystemLog> findByLevelAndSourceOrderByTimestampDesc(String level, String source, Pageable pageable);

    void deleteByTimestampBefore(Instant cutoff);
}
