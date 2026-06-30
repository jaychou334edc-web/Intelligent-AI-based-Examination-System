package com.aes.exam.monitoring.vo;

import java.time.LocalDateTime;

public record EventRecordedVO(
    Long eventId,
    String status,
    LocalDateTime recordedAt
) {
}
