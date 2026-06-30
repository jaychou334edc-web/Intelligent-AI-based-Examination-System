package com.aes.exam.exam.dto;

import jakarta.validation.constraints.NotNull;

public record SaveAnswerRequest(
    @NotNull Long questionId,
    String answer
) {
}
