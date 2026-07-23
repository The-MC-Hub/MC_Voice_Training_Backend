package com.mchub.repositories;

import com.mchub.models.SearchInterest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SearchInterestRepository extends MongoRepository<SearchInterest, String> {

    List<SearchInterest> findByClientId(String clientId);

    Optional<SearchInterest> findByClientIdAndKeyword(String clientId, String keyword);

    void deleteByClientId(String clientId);
}
