package com.aes.exam.paper.repository;

import com.aes.exam.paper.entity.PaperEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPaperRepository implements PaperRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPaperRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long create(String title, String filePath, String fileName, String fileHash, long fileSize, Long uploadUserId, LocalDateTime uploadTime) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO papers (
                    title, file_path, file_name, file_hash, file_size, upload_user_id, upload_time,
                    parse_status, created_at, updated_at, created_by, updated_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
                """, new String[]{"id"});
            statement.setString(1, title);
            statement.setString(2, filePath);
            statement.setString(3, fileName);
            statement.setString(4, fileHash);
            statement.setLong(5, fileSize);
            statement.setLong(6, uploadUserId);
            statement.setObject(7, uploadTime);
            statement.setLong(8, uploadUserId);
            statement.setLong(9, uploadUserId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public Optional<PaperEntity> findById(Long id) {
        return jdbcTemplate.query("""
                SELECT id, title, file_path, file_name, file_hash, file_size, upload_user_id, upload_time,
                       parse_status, ai_model, raw_text
                FROM papers
                WHERE id = ? AND is_deleted = 0
                """,
            this::mapPaper,
            id
        ).stream().findFirst();
    }

    @Override
    public void updateRawTextAndStatus(Long id, String rawText, String parseStatus, String aiModel) {
        jdbcTemplate.update("""
            UPDATE papers
            SET raw_text = ?, parse_status = ?, ai_model = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, rawText, parseStatus, aiModel, id);
    }

    private PaperEntity mapPaper(ResultSet rs, int rowNum) throws SQLException {
        return new PaperEntity(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("file_path"),
            rs.getString("file_name"),
            rs.getString("file_hash"),
            rs.getLong("file_size"),
            rs.getLong("upload_user_id"),
            rs.getTimestamp("upload_time").toLocalDateTime(),
            rs.getString("parse_status"),
            rs.getString("ai_model"),
            rs.getString("raw_text")
        );
    }
}
