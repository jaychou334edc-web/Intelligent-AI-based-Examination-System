package com.aes.exam.monitoring.vo;

public record EventCountVO(
    String eventType,
    Long count
) {
}
