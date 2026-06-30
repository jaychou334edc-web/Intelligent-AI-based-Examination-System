package com.aes.exam.ai.repository;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAiParseJobRepository implements AiParseJobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAiParseJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long create(Long paperId, String status, String parserType, String aiModel, LocalDateTime startedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO ai_parse_jobs (paper_id, status, parser_type, ai_model, started_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, new String[]{"id"});
            statement.setLong(1, paperId);
            statement.setString(2, status);
            statement.setString(3, parserType);
            statement.setString(4, aiModel);
            statement.setObject(5, startedAt);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public void complete(Long jobId, String status, String requestPayload, String responsePayload, String errorMessage, LocalDateTime finishedAt) {
        jdbcTemplate.update("""
            UPDATE ai_parse_jobs
            SET status = ?, request_payload = ?, response_payload = ?, error_message = ?, finished_at = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, status, requestPayload, responsePayload, errorMessage, finishedAt, jobId);
    }
}
