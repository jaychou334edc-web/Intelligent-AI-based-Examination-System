package com.aes.exam.ai.repository;

import java.time.LocalDateTime;

public interface AiParseJobRepository {

    Long create(Long paperId, String status, String parserType, String aiModel, LocalDateTime startedAt);

    void complete(Long jobId, String status, String requestPayload, String responsePayload, String errorMessage, LocalDateTime finishedAt);
}
