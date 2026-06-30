package com.aes.exam.auth.repository;

import com.aes.exam.auth.entity.LoginSessionEntity;
import java.time.LocalDateTime;
import java.util.Optional;

public interface LoginSessionRepository {

    void create(Long userId, String tokenHash, LocalDateTime expiredAt, String userAgent, String ipAddress);

    Optional<LoginSessionEntity> findByTokenHash(String tokenHash);

    void revoke(String tokenHash, LocalDateTime revokedAt);
}
