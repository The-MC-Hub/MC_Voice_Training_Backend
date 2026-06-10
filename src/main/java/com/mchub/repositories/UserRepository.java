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

    // Non-admin queries (exclude ADMIN role from stats)
    List<User> findByRoleNot(UserRole role);

    long countByRoleNot(UserRole role);

    long countByPlanAndRoleNot(SubscriptionPlan plan, UserRole role);

    long countByIsPremiumTrueAndRoleNot(UserRole role);

    List<User> findByCreatedAtAfterAndRoleNot(LocalDateTime after, UserRole role);

    List<User> findByCreatedAtBetweenAndRoleNot(LocalDateTime from, LocalDateTime to, UserRole role);

    List<User> findByPlanIn(List<SubscriptionPlan> plans);

    List<User> findByRoleIn(List<UserRole> roles);

    List<User> findByIsPremiumTrue();

    List<User> findByEmailIn(List<String> emails);
}
