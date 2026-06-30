package com.aes.exam.auth.entity;

import com.aes.exam.auth.UserRole;
import com.aes.exam.auth.UserStatus;
import java.time.LocalDateTime;

public record UserEntity(
    Long id,
    String username,
    String passwordHash,
    UserRole role,
    UserStatus status,
    String realName,
    LocalDateTime lastLoginAt
) {
}
