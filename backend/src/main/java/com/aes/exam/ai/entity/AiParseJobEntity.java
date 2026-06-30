package com.aes.exam.ai.entity;

import java.time.LocalDateTime;

public record AiParseJobEntity(
    Long id,
    Long paperId,
    String status,
    String parserType,
    String aiModel,
    String requestPayload,
    String responsePayload,
    String errorMessage,
    int retryCount,
    LocalDateTime startedAt,
    LocalDateTime finishedAt
) {
}
