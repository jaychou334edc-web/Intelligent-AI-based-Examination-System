CREATE TABLE IF NOT EXISTS anti_cheat_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_level VARCHAR(32) NOT NULL,
    event_data LONGTEXT NULL,
    client_time DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_anti_cheat_events_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_anti_cheat_events_exam FOREIGN KEY (exam_id) REFERENCES exams(id)
);

CREATE TABLE IF NOT EXISTS exam_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    average_score DECIMAL(8,2) NULL,
    max_score DECIMAL(8,2) NULL,
    min_score DECIMAL(8,2) NULL,
    pass_rate DECIMAL(5,2) NULL,
    submitted_count INT NOT NULL DEFAULT 0,
    participant_count INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exam_statistics_exam FOREIGN KEY (exam_id) REFERENCES exams(id),
    CONSTRAINT uk_exam_statistics_exam UNIQUE (exam_id)
);

CREATE INDEX idx_anti_cheat_events_exam ON anti_cheat_events(exam_id, created_at);
CREATE INDEX idx_anti_cheat_events_user ON anti_cheat_events(user_id, exam_id);
CREATE INDEX idx_anti_cheat_events_type ON anti_cheat_events(event_type);
