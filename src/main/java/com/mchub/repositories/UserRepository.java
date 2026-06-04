package com.mchub.repositories;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.UserRole;
import com.mchub.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    List<User> findByRole(UserRole role);

    List<User> findByCreatedAtAfter(LocalDateTime after);

    List<User> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByPlan(SubscriptionPlan plan);

    long countByIsPremiumTrue();

    List<User> findAllByOrderByCreatedAtDesc();
}
