package com.aes.exam.common.security;

import com.aes.exam.auth.UserRole;

public record SecurityContext(Long userId, String username, UserRole role, String realName) {
}
