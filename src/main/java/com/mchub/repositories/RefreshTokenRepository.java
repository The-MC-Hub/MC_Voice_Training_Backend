package com.mchub.repositories;

import com.mchub.models.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserId(String userId);
    List<RefreshToken> findByUserIdAndIsRevoked(String userId, boolean isRevoked);
    void deleteByUserId(String userId);
    void deleteByToken(String token);
}
