package com.mchub.repositories;

import com.mchub.models.CaseStudy;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CaseStudyRepository extends MongoRepository<CaseStudy, String> {
}
