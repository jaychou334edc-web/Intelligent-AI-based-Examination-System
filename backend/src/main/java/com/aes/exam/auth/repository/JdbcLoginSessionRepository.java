package com.aes.exam.auth.repository;

import com.aes.exam.auth.entity.LoginSessionEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLoginSessionRepository implements LoginSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLoginSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(Long userId, String tokenHash, LocalDateTime expiredAt, String userAgent, String ipAddress) {
        jdbcTemplate.update("""
            INSERT INTO login_sessions (user_id, token_hash, user_agent, ip_address, expired_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, userId, tokenHash, userAgent, ipAddress, expiredAt);
    }

    @Override
    public Optional<LoginSessionEntity> findByTokenHash(String tokenHash) {
        return jdbcTemplate.query("""
                SELECT id, user_id, token_hash, expired_at, revoked_at
                FROM login_sessions
                WHERE token_hash = ?
                """,
            this::mapSession,
            tokenHash
        ).stream().findFirst();
    }

    @Override
    public void revoke(String tokenHash, LocalDateTime revokedAt) {
        jdbcTemplate.update("""
            UPDATE login_sessions SET revoked_at = ?, updated_at = CURRENT_TIMESTAMP WHERE token_hash = ?
            """, revokedAt, tokenHash);
    }

    private LoginSessionEntity mapSession(ResultSet rs, int rowNum) throws SQLException {
        return new LoginSessionEntity(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("token_hash"),
            rs.getTimestamp("expired_at").toLocalDateTime(),
            rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toLocalDateTime()
        );
    }
}
