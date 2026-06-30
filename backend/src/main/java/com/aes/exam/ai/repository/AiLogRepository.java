package com.aes.exam.ai.repository;

public interface AiLogRepository {

    void create(Long paperId, Long parseJobId, String request, String response, String model, String provider);
}
