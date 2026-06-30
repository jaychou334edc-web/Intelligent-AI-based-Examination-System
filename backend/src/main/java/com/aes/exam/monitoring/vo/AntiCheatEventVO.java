package com.aes.exam.monitoring.vo;

import java.time.LocalDateTime;
import java.util.Map;

public record AntiCheatEventVO(
    Long id,
    Long userId,
    String studentName,
    Long examId,
    String examTitle,
    String eventType,
    String eventLevel,
    Map<String, Object> eventData,
    LocalDateTime clientTime,
    LocalDateTime createdAt
) {
}
