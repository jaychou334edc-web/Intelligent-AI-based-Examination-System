package com.aes.exam.ai.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAiLogRepository implements AiLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAiLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(Long paperId, Long parseJobId, String request, String response, String model, String provider) {
        jdbcTemplate.update("""
            INSERT INTO ai_logs (paper_id, parse_job_id, request, response, model, provider, created_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """, paperId, parseJobId, request, response, model, provider);
    }
}
