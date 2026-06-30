CREATE TABLE IF NOT EXISTS papers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NULL,
    file_path TEXT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_hash VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_user_id BIGINT NOT NULL,
    upload_time DATETIME NOT NULL,
    parse_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    ai_model VARCHAR(64) NULL,
    raw_text LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_papers_upload_user FOREIGN KEY (upload_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS ai_parse_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    parser_type VARCHAR(32) NOT NULL,
    ai_model VARCHAR(64) NULL,
    request_payload LONGTEXT NULL,
    response_payload LONGTEXT NULL,
    error_message TEXT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_parse_jobs_paper FOREIGN KEY (paper_id) REFERENCES papers(id)
);

CREATE TABLE IF NOT EXISTS ai_parsed_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NOT NULL,
    parse_job_id BIGINT NOT NULL,
    question_json LONGTEXT NOT NULL,
    confidence_score DECIMAL(5,2) NULL,
    is_reviewed TINYINT(1) NOT NULL DEFAULT 0,
    review_status VARCHAR(32) NULL,
    review_comment TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_parsed_questions_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
    CONSTRAINT fk_ai_parsed_questions_job FOREIGN KEY (parse_job_id) REFERENCES ai_parse_jobs(id)
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_paper_id BIGINT NULL,
    source_ai_question_id BIGINT NULL,
    question_type VARCHAR(32) NOT NULL,
    stem TEXT NOT NULL,
    analysis TEXT NULL,
    score DECIMAL(6,2) NOT NULL,
    difficulty VARCHAR(32) NULL,
    knowledge_point VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_questions_source_paper FOREIGN KEY (source_paper_id) REFERENCES papers(id),
    CONSTRAINT fk_questions_source_ai FOREIGN KEY (source_ai_question_id) REFERENCES ai_parsed_questions(id)
);

CREATE TABLE IF NOT EXISTS question_options (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    option_key VARCHAR(8) NOT NULL,
    option_text TEXT NOT NULL,
    is_correct TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_question_options_question FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE TABLE IF NOT EXISTS question_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    answer_text TEXT NULL,
    answer_hash VARCHAR(255) NULL,
    match_rule VARCHAR(32) NOT NULL DEFAULT 'exact',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_question_answers_question FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE TABLE IF NOT EXISTS ai_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NULL,
    parse_job_id BIGINT NULL,
    request TEXT NULL,
    response LONGTEXT NULL,
    model VARCHAR(64) NULL,
    provider VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_logs_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
    CONSTRAINT fk_ai_logs_parse_job FOREIGN KEY (parse_job_id) REFERENCES ai_parse_jobs(id)
);

CREATE INDEX idx_papers_file_hash ON papers(file_hash);
CREATE INDEX idx_papers_parse_status ON papers(parse_status);
CREATE INDEX idx_questions_source_paper ON questions(source_paper_id);
CREATE INDEX idx_ai_parse_jobs_paper ON ai_parse_jobs(paper_id);
