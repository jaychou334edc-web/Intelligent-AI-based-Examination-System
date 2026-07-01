package com.aes.exam.grading.vo;

import java.math.BigDecimal;

public record AiGradeSuggestionVO(
    Long submissionId,
    Long questionId,
    BigDecimal suggestedScore,
    String reason
) {
}
