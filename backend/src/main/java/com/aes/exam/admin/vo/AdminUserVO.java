package com.aes.exam.admin.vo;

import java.time.LocalDateTime;

public record AdminUserVO(
    Long id,
    String username,
    String role,
    String status,
    String realName,
    LocalDateTime lastLoginAt
) {
}
