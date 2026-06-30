package com.aes.exam.ai.repository;

import com.aes.exam.ai.entity.AiParsedQuestionEntity;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAiParsedQuestionRepository implements AiParsedQuestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAiParsedQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void deleteByPaperId(Long paperId) {
        jdbcTemplate.update("DELETE FROM ai_parsed_questions WHERE paper_id = ?", paperId);
    }

    @Override
    public Long create(Long paperId, Long parseJobId, String questionJson, BigDecimal confidenceScore, String reviewStatus) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO ai_parsed_questions (
                    paper_id, parse_job_id, question_json, confidence_score, is_reviewed,
                    review_status, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, new String[]{"id"});
            statement.setLong(1, paperId);
            statement.setLong(2, parseJobId);
            statement.setString(3, questionJson);
            statement.setBigDecimal(4, confidenceScore);
            statement.setString(5, reviewStatus);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public List<AiParsedQuestionEntity> findByPaperId(Long paperId) {
        return jdbcTemplate.query("""
                SELECT id, paper_id, parse_job_id, question_json, confidence_score, is_reviewed, review_status, review_comment
                FROM ai_parsed_questions
                WHERE paper_id = ?
                ORDER BY id
                """,
            this::mapQuestion,
            paperId
        );
    }

    @Override
    public void markReviewed(Long id, String reviewStatus, String reviewComment, String questionJson) {
        jdbcTemplate.update("""
            UPDATE ai_parsed_questions
            SET is_reviewed = 1, review_status = ?, review_comment = ?, question_json = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, reviewStatus, reviewComment, questionJson, id);
    }

    private AiParsedQuestionEntity mapQuestion(ResultSet rs, int rowNum) throws SQLException {
        return new AiParsedQuestionEntity(
            rs.getLong("id"),
            rs.getLong("paper_id"),
            rs.getLong("parse_job_id"),
            rs.getString("question_json"),
            rs.getBigDecimal("confidence_score"),
            rs.getBoolean("is_reviewed"),
            rs.getString("review_status"),
            rs.getString("review_comment")
        );
    }
}
