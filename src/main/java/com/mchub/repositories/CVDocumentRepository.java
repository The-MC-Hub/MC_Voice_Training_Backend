package com.mchub.repositories;

import com.mchub.models.CVDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CVDocumentRepository extends MongoRepository<CVDocument, String> {
  List<CVDocument> findByUserIdOrderByUploadedAtDesc(String userId);
}
