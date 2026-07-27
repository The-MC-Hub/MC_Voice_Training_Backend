package com.mchub.repositories;

import com.mchub.models.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends MongoRepository<Favorite, String> {
  List<Favorite> findByClientId(String clientId);

  List<Favorite> findByMcUserId(String mcUserId);

  Optional<Favorite> findByClientIdAndMcUserId(String clientId, String mcUserId);

  boolean existsByClientIdAndMcUserId(String clientId, String mcUserId);

  void deleteByClientIdAndMcUserId(String clientId, String mcUserId);

  long countByMcUserId(String mcUserId);
}
