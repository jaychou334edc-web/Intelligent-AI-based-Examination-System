package com.aes.exam.grading.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubmissionSummaryVO(
    Long submissionId,
    Long examId,
    String examTitle,
    Long studentId,
    String studentName,
    String submissionStatus,
    BigDecimal totalScore,
    BigDecimal maxScore,
    LocalDateTime submittedAt,
    LocalDateTime gradedAt
) {
}
