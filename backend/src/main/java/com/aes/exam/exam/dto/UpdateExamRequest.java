package com.aes.exam.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateExamRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 2000) String description,
    @NotNull @Min(1) @Max(600) Integer durationMinutes
) {
}
