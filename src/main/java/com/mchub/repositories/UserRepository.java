package com.mchub.repositories;

import com.mchub.enums.UserRole;
import com.mchub.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    
    Optional<User> findByEmail(String email);
    
    
    boolean existsByEmail(String email);

    
    long countByRole(UserRole role);

    
    List<User> findByRole(UserRole role);
}
