package com.aes.exam.monitoring.vo;

import java.math.BigDecimal;

public record KnowledgePointStatsVO(
    String knowledgePoint,
    Long questionCount,
    BigDecimal averageScore,
    BigDecimal accuracyRate
) {
}
