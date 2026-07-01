package com.aes.exam.admin.repository;

import com.aes.exam.admin.vo.LoginSessionAuditVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAdminRepository implements AdminRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<LoginSessionAuditVO> findRecentSessions(int limit) {
        return jdbcTemplate.query("""
                SELECT ls.id, ls.user_id, u.username, u.role, ls.user_agent, ls.ip_address,
                       ls.expired_at, ls.revoked_at, ls.created_at
                FROM login_sessions ls
                JOIN users u ON u.id = ls.user_id
                ORDER BY ls.created_at DESC, ls.id DESC
                LIMIT ?
                """, this::mapSession, Math.max(1, Math.min(limit, 100)));
    }

    private LoginSessionAuditVO mapSession(ResultSet rs, int rowNum) throws SQLException {
        return new LoginSessionAuditVO(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("username"),
            rs.getString("role"),
            rs.getString("user_agent"),
            rs.getString("ip_address"),
            toLocalDateTime(rs.getTimestamp("expired_at")),
            toLocalDateTime(rs.getTimestamp("revoked_at")),
            toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
