package com.aes.exam.question.repository;

import com.aes.exam.question.dto.QuestionOptionRequest;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcQuestionRepository implements QuestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long createQuestion(
        Long sourcePaperId,
        Long sourceAiQuestionId,
        String questionType,
        String stem,
        String analysis,
        BigDecimal score,
        String difficulty,
        String knowledgePoint,
        Long currentUserId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO questions (
                    source_paper_id, source_ai_question_id, question_type, stem, analysis, score, difficulty,
                    knowledge_point, status, version, created_at, updated_at, created_by, updated_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'active', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
                """, new String[]{"id"});
            statement.setObject(1, sourcePaperId);
            statement.setObject(2, sourceAiQuestionId);
            statement.setString(3, questionType);
            statement.setString(4, stem);
            statement.setString(5, analysis);
            statement.setBigDecimal(6, score);
            statement.setString(7, difficulty);
            statement.setString(8, knowledgePoint);
            statement.setLong(9, currentUserId);
            statement.setLong(10, currentUserId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public void createOptions(Long questionId, List<QuestionOptionRequest> options, String answer) {
        if (options == null || options.isEmpty()) {
            return;
        }

        Set<String> correctKeys = answerKeys(answer);
        for (int index = 0; index < options.size(); index++) {
            QuestionOptionRequest option = options.get(index);
            String key = option.key().trim().toUpperCase(Locale.ROOT);
            jdbcTemplate.update("""
                INSERT INTO question_options (question_id, option_key, option_text, is_correct, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, questionId, key, option.text(), correctKeys.contains(key), index);
        }
    }

    @Override
    public void createAnswer(Long questionId, String answer) {
        jdbcTemplate.update("""
            INSERT INTO question_answers (question_id, answer_text, match_rule, created_at, updated_at)
            VALUES (?, ?, 'exact', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, questionId, answer);
    }

    private Set<String> answerKeys(String answer) {
        Set<String> keys = new HashSet<>();
        if (answer == null) {
            return keys;
        }
        for (String value : answer.toUpperCase(Locale.ROOT).split("[,，、\\s]+")) {
            if (!value.isBlank()) {
                keys.add(value.trim());
            }
        }
        return keys;
    }
}
