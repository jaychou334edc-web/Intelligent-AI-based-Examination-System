package com.aes.exam.auth.repository;

import com.aes.exam.auth.UserRole;
import com.aes.exam.auth.UserStatus;
import com.aes.exam.auth.entity.UserEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepository implements UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return jdbcTemplate.query("""
                SELECT u.id, u.username, u.password_hash, u.role, u.status, p.real_name, u.last_login_at
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id AND p.is_deleted = 0
                WHERE u.username = ? AND u.is_deleted = 0
                """,
            this::mapUser,
            username
        ).stream().findFirst();
    }

    @Override
    public Optional<UserEntity> findById(Long id) {
        return jdbcTemplate.query("""
                SELECT u.id, u.username, u.password_hash, u.role, u.status, p.real_name, u.last_login_at
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id AND p.is_deleted = 0
                WHERE u.id = ? AND u.is_deleted = 0
                """,
            this::mapUser,
            id
        ).stream().findFirst();
    }

    @Override
    public Long createUser(String username, String passwordHash, UserRole role, String realName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO users (username, password_hash, role, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, new String[]{"id"});
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, role.value());
            statement.setString(4, UserStatus.ACTIVE.value());
            return statement;
        }, keyHolder);

        Long userId = keyHolder.getKey().longValue();
        jdbcTemplate.update("""
            INSERT INTO user_profiles (user_id, real_name, created_at, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, userId, realName);
        return userId;
    }

    @Override
    public void updateLastLoginAt(Long userId, LocalDateTime lastLoginAt) {
        jdbcTemplate.update("""
            UPDATE users SET last_login_at = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
            """, lastLoginAt, userId);
    }

    private UserEntity mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new UserEntity(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            UserRole.fromValue(rs.getString("role")),
            UserStatus.fromValue(rs.getString("status")),
            rs.getString("real_name"),
            rs.getTimestamp("last_login_at") == null ? null : rs.getTimestamp("last_login_at").toLocalDateTime()
        );
    }
}
