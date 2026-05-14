package com.animalplatform.user.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByName(String name);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    java.util.List<User> findAllByAllowEmailNotificationTrue();
}
