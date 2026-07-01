package com.aes.exam.academic.vo;

import java.time.LocalDateTime;

public record CourseVO(
    Long id,
    String name,
    String code,
    String description,
    Long teacherId,
    String teacherName,
    String status,
    Integer classCount,
    Integer studentCount,
    LocalDateTime createdAt
) {
}
