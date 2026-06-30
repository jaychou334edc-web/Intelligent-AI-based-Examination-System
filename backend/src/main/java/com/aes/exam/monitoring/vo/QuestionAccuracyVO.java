package com.aes.exam.monitoring.vo;

import java.math.BigDecimal;

public record QuestionAccuracyVO(
    Long questionId,
    String questionType,
    String stem,
    BigDecimal maxScore,
    BigDecimal averageScore,
    BigDecimal accuracyRate
) {
}
