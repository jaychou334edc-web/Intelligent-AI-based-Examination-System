package com.aes.exam.exam.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubmitExamVO(
    Long submissionId,
    String status,
    BigDecimal totalScore,
    LocalDateTime submittedAt
) {
}
