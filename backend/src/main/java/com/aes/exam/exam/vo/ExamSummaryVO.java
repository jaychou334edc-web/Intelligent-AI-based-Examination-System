package com.aes.exam.exam.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamSummaryVO(
    Long id,
    String title,
    String description,
    Integer durationMinutes,
    Long courseId,
    Long classId,
    String courseName,
    String className,
    String status,
    Integer questionCount,
    BigDecimal totalScore,
    LocalDateTime publishedAt,
    LocalDateTime startTime,
    LocalDateTime endTime,
    LocalDateTime createdAt,
    String submissionStatus
) {
}
