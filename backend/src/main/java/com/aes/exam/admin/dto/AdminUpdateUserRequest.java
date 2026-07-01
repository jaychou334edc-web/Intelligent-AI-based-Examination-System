package com.aes.exam.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequest(
    @NotBlank @Pattern(regexp = "admin|teacher|student") String role,
    @NotBlank @Pattern(regexp = "active|disabled") String status,
    @NotBlank @Size(max = 64) String realName
) {
}
