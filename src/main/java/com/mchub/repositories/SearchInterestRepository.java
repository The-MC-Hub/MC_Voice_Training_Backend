package com.mchub.repositories;

import com.mchub.models.SearchInterest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SearchInterestRepository extends MongoRepository<SearchInterest, String> {

  List<SearchInterest> findByClientId(String clientId);

  Optional<SearchInterest> findByClientIdAndKeyword(String clientId, String keyword);

  void deleteByClientId(String clientId);
}
