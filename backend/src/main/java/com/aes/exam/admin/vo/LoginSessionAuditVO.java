package com.aes.exam.admin.vo;

import java.time.LocalDateTime;

public record LoginSessionAuditVO(
    Long id,
    Long userId,
    String username,
    String role,
    String userAgent,
    String ipAddress,
    LocalDateTime expiredAt,
    LocalDateTime revokedAt,
    LocalDateTime createdAt
) {
}
