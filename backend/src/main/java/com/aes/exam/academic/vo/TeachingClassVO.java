package com.aes.exam.academic.vo;

import java.time.LocalDateTime;

public record TeachingClassVO(
    Long id,
    Long courseId,
    String courseName,
    String name,
    String grade,
    String major,
    String status,
    Integer studentCount,
    LocalDateTime createdAt
) {
}
