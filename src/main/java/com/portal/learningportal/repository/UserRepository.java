package com.portal.learningportal.repository;

import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    default Optional<User> findByUsernameOrEmail(String login) {
        return findByUsername(login).or(() -> findByEmail(login));
    }

    List<User> findByRole(Role role);
}