package com.mchub.repositories;

import com.mchub.models.ScriptRevision;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScriptRevisionRepository extends MongoRepository<ScriptRevision, String> {
  List<ScriptRevision> findByBookingIdOrderByRevisionNumberDesc(String bookingId);
}
