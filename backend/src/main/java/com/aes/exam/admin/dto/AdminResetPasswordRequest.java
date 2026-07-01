package com.aes.exam.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
    @NotBlank @Size(min = 8, max = 64) String password
) {
}
