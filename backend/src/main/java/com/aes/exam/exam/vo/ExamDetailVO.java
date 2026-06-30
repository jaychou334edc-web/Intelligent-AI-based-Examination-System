package com.aes.exam.exam.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExamDetailVO(
    Long id,
    String title,
    String description,
    Integer durationMinutes,
    String status,
    Integer questionCount,
    BigDecimal totalScore,
    LocalDateTime publishedAt,
    List<ExamQuestionVO> questions,
    Long submissionId,
    String submissionStatus,
    LocalDateTime submissionStartedAt
) {
}
