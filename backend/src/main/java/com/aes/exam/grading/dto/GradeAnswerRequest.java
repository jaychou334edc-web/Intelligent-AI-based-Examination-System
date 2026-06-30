package com.aes.exam.grading.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GradeAnswerRequest(
    @NotNull Long questionId,
    @NotNull @DecimalMin("0.0") BigDecimal score,
    String teacherComment
) {
}
