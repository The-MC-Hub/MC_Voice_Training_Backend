package com.mchub.repositories;

import com.mchub.models.ScriptDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScriptDocumentRepository extends MongoRepository<ScriptDocument, String> {
  Optional<ScriptDocument> findByBookingId(String bookingId);
}
