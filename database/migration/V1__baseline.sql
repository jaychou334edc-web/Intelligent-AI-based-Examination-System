CREATE TABLE IF NOT EXISTS schema_version_marker (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_name VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_version_marker (version_name, description)
VALUES ('v0.1.0-phase0', 'Phase 0 baseline schema')
ON DUPLICATE KEY UPDATE description = VALUES(description);
