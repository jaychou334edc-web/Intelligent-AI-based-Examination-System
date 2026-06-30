CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    real_name VARCHAR(64) NULL,
    student_number VARCHAR(64) NULL,
    employee_number VARCHAR(64) NULL,
    department VARCHAR(128) NULL,
    class_name VARCHAR(128) NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS login_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_agent VARCHAR(512) NULL,
    ip_address VARCHAR(64) NULL,
    expired_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_login_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_login_sessions_user_id ON login_sessions(user_id);
CREATE INDEX idx_login_sessions_expired_at ON login_sessions(expired_at);
