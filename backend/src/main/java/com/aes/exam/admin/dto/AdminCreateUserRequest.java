package com.aes.exam.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
    @NotBlank @Size(max = 64) String username,
    @NotBlank @Size(min = 8, max = 64) String password,
    @NotBlank @Pattern(regexp = "admin|teacher|student") String role,
    @NotBlank @Size(max = 64) String realName
) {
}
