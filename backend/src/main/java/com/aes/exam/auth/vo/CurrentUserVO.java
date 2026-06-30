package com.aes.exam.auth.vo;

import com.aes.exam.auth.UserRole;

public record CurrentUserVO(
    Long id,
    String username,
    UserRole role,
    String realName
) {
}
