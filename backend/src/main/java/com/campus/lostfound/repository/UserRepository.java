package com.campus.lostfound.repository;

import com.campus.lostfound.entity.Role;
import com.campus.lostfound.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByRollNumberIgnoreCase(String rollNumber);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRollNumberIgnoreCase(String rollNumber);
    List<User> findByRole(Role role);
}
