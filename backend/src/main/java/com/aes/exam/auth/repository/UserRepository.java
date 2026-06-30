package com.aes.exam.auth.repository;

import com.aes.exam.auth.UserRole;
import com.aes.exam.auth.entity.UserEntity;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findById(Long id);

    Long createUser(String username, String passwordHash, UserRole role, String realName);

    void updateLastLoginAt(Long userId, LocalDateTime lastLoginAt);
}
