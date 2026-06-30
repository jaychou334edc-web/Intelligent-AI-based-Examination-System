package com.aes.exam.monitoring.vo;

import java.math.BigDecimal;

public record DifficultyStatsVO(
    String difficulty,
    Long questionCount,
    BigDecimal averageScore,
    BigDecimal accuracyRate
) {
}
