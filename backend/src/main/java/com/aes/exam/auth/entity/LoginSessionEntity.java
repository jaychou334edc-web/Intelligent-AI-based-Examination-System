package com.aes.exam.auth.entity;

import java.time.LocalDateTime;

public record LoginSessionEntity(
    Long id,
    Long userId,
    String tokenHash,
    LocalDateTime expiredAt,
    LocalDateTime revokedAt
) {

    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && expiredAt.isAfter(now);
    }
}
