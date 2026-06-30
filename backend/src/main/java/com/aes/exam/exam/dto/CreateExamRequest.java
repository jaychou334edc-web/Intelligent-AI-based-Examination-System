package com.aes.exam.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateExamRequest(
    @NotBlank String title,
    String description,
    @Min(1) @Max(600) Integer durationMinutes
) {
}
