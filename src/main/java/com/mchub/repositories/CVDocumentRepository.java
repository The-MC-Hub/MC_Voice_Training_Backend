package com.mchub.repositories;

import com.mchub.models.CVDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CVDocumentRepository extends MongoRepository<CVDocument, String> {
    List<CVDocument> findByUserIdOrderByUploadedAtDesc(String userId);
}
