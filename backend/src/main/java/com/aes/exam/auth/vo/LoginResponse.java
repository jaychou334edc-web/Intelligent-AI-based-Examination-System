package com.aes.exam.auth.vo;

public record LoginResponse(
    String token,
    CurrentUserVO user
) {
}
