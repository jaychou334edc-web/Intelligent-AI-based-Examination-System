ALTER TABLE submissions
    ADD COLUMN graded_at DATETIME NULL;

CREATE TABLE IF NOT EXISTS grading_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    auto_score DECIMAL(8,2) NULL,
    manual_score DECIMAL(8,2) NULL,
    final_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    grader_id BIGINT NULL,
    ai_suggestion_score DECIMAL(8,2) NULL,
    ai_comment TEXT NULL,
    teacher_comment TEXT NULL,
    grading_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    graded_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_grading_records_submission FOREIGN KEY (submission_id) REFERENCES submissions(id),
    CONSTRAINT fk_grading_records_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT fk_grading_records_grader FOREIGN KEY (grader_id) REFERENCES users(id),
    CONSTRAINT uk_grading_records_question UNIQUE (submission_id, question_id)
);

CREATE INDEX idx_grading_records_submission ON grading_records(submission_id);
CREATE INDEX idx_grading_records_status ON grading_records(grading_status);
