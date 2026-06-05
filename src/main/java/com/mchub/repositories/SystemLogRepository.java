package com.mchub.repositories;

import com.mchub.models.SystemLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SystemLogRepository extends MongoRepository<SystemLog, String> {

    List<SystemLog> findTop200ByOrderByTimestampDesc();

    List<SystemLog> findTop200ByLevelOrderByTimestampDesc(String level);

    List<SystemLog> findTop200BySourceOrderByTimestampDesc(String source);

    List<SystemLog> findTop200ByLevelAndSourceOrderByTimestampDesc(String level, String source);

    void deleteByTimestampBefore(Instant cutoff);
}
