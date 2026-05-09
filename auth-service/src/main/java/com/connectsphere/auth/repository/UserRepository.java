package com.connectsphere.auth.repository;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.entity.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence access for User data.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByProviderId(String providerId);

    List<User> findAllByRole(Role role);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long countByIsActiveTrue();

    long countByUpdatedAtAfter(Instant updatedAt);

}
