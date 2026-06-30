package com.aes.exam.ai.entity;

import java.math.BigDecimal;

public record AiParsedQuestionEntity(
    Long id,
    Long paperId,
    Long parseJobId,
    String questionJson,
    BigDecimal confidenceScore,
    boolean reviewed,
    String reviewStatus,
    String reviewComment
) {
}
