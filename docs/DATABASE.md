# AI Examination System (AES)

# Database Design Specification

Version: 2.0

Status: Approved

Audience:

- Codex
- Backend Developers
- Database Engineers
- System Architects

This document defines the complete relational data model of AES.

All business logic must conform to this schema.

No field or table may be modified without updating this document.

# 1. Database Architecture Overview

AES uses a relational database model.

Supported engine:

- MySQL 8+

Design Principles:

- Normalize core entities
- Use soft delete for critical data
- Use audit fields for traceability
- Avoid JSON storage for core business entities except AI raw output and browser event metadata
- All time fields stored in UTC
- Backend is the only component allowed to access the database

# 2. Core Design Rules

1. No table is allowed to store business logic.
2. AI outputs are stored separately from validated data.
3. Deleting a record must never break historical grading.
4. Every critical entity must include audit fields.
5. Relationships must use foreign keys unless explicitly performance-critical.
6. Browser clients must never connect to MySQL.

# 3. Common Base Fields

All core tables must include:

```sql
id              BIGINT PRIMARY KEY AUTO_INCREMENT,
created_at      DATETIME NOT NULL,
updated_at      DATETIME NOT NULL,
created_by      BIGINT NULL,
updated_by      BIGINT NULL,
is_deleted      TINYINT(1) NOT NULL DEFAULT 0
```

# 4. User & Auth Domain

## 4.1 users

Stores all system users.

```sql
users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    last_login_at DATETIME NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0
)
```

Role values:

- admin
- teacher
- student

## 4.2 user_profiles

```sql
user_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,

    real_name VARCHAR(64),
    student_number VARCHAR(64),
    employee_number VARCHAR(64),
    department VARCHAR(128),
    class_name VARCHAR(128),

    phone VARCHAR(32),
    email VARCHAR(128),

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,

    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id)
)
```

## 4.3 login_sessions

```sql
login_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    user_agent VARCHAR(512),
    ip_address VARCHAR(64),
    expired_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_login_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
)
```

# 5. Paper Domain

## 5.1 papers

Stores original uploaded Word files.

```sql
papers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255),

    file_path TEXT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_hash VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,

    upload_user_id BIGINT NOT NULL,
    upload_time DATETIME NOT NULL,

    parse_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    ai_model VARCHAR(64),

    raw_text LONGTEXT,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,

    CONSTRAINT fk_papers_upload_user FOREIGN KEY (upload_user_id) REFERENCES users(id)
)
```

Parse status values:

- pending
- parsing
- parsed
- failed
- reviewed

# 6. AI Parsing Domain

## 6.1 ai_parse_jobs

Tracks AI parsing tasks.

```sql
ai_parse_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NOT NULL,

    status VARCHAR(32) NOT NULL,
    parser_type VARCHAR(32) NOT NULL,
    ai_model VARCHAR(64),

    request_payload LONGTEXT,
    response_payload LONGTEXT,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,

    started_at DATETIME,
    finished_at DATETIME,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_ai_parse_jobs_paper FOREIGN KEY (paper_id) REFERENCES papers(id)
)
```

Parser type values:

- java
- python_service

## 6.2 ai_parsed_questions

Temporary layer storing raw AI extraction before validation.

```sql
ai_parsed_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NOT NULL,
    parse_job_id BIGINT NOT NULL,

    question_json LONGTEXT NOT NULL,
    confidence_score DECIMAL(5,2),

    is_reviewed TINYINT(1) NOT NULL DEFAULT 0,
    review_status VARCHAR(32),
    review_comment TEXT,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_ai_parsed_questions_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
    CONSTRAINT fk_ai_parsed_questions_job FOREIGN KEY (parse_job_id) REFERENCES ai_parse_jobs(id)
)
```

Review status values:

- approved
- rejected
- modified
- needs_review

# 7. Question Bank Domain

## 7.1 questions

Core validated question storage.

```sql
questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    source_paper_id BIGINT NULL,
    source_ai_question_id BIGINT NULL,

    question_type VARCHAR(32) NOT NULL,
    stem TEXT NOT NULL,
    analysis TEXT,

    score DECIMAL(6,2) NOT NULL,
    difficulty VARCHAR(32),
    knowledge_point VARCHAR(255),

    status VARCHAR(32) NOT NULL DEFAULT 'active',
    version INT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,

    CONSTRAINT fk_questions_source_paper FOREIGN KEY (source_paper_id) REFERENCES papers(id),
    CONSTRAINT fk_questions_source_ai FOREIGN KEY (source_ai_question_id) REFERENCES ai_parsed_questions(id)
)
```

Question type values:

- single_choice
- multiple_choice
- true_false
- fill_blank
- subjective

## 7.2 question_options

```sql
question_options (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,

    option_key VARCHAR(8) NOT NULL,
    option_text TEXT NOT NULL,
    is_correct TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_question_options_question FOREIGN KEY (question_id) REFERENCES questions(id)
)
```

## 7.3 question_answers

```sql
question_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,

    answer_text TEXT,
    answer_hash VARCHAR(255),
    match_rule VARCHAR(32) NOT NULL DEFAULT 'exact',

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_question_answers_question FOREIGN KEY (question_id) REFERENCES questions(id)
)
```

# 8. Exam Domain

## 8.1 exams

```sql
exams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    title VARCHAR(255) NOT NULL,
    description TEXT,

    creator_id BIGINT NOT NULL,

    start_time DATETIME,
    end_time DATETIME,
    duration_minutes INT NOT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    anti_cheat_level VARCHAR(32) NOT NULL DEFAULT 'normal',

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,

    CONSTRAINT fk_exams_creator FOREIGN KEY (creator_id) REFERENCES users(id)
)
```

Status values:

- draft
- published
- in_progress
- finished
- archived

## 8.2 exam_questions

```sql
exam_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,

    score_override DECIMAL(6,2) NULL,
    sort_order INT NOT NULL DEFAULT 0,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_exam_questions_exam FOREIGN KEY (exam_id) REFERENCES exams(id),
    CONSTRAINT fk_exam_questions_question FOREIGN KEY (question_id) REFERENCES questions(id)
)
```

## 8.3 exam_participants

```sql
exam_participants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'not_started',

    start_time DATETIME,
    submit_time DATETIME,
    last_active_at DATETIME,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_exam_participants_exam FOREIGN KEY (exam_id) REFERENCES exams(id),
    CONSTRAINT fk_exam_participants_user FOREIGN KEY (user_id) REFERENCES users(id)
)
```

# 9. Submission Domain

## 9.1 submissions

```sql
submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    total_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'submitted',

    submit_time DATETIME,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_submissions_exam FOREIGN KEY (exam_id) REFERENCES exams(id),
    CONSTRAINT fk_submissions_user FOREIGN KEY (user_id) REFERENCES users(id)
)
```

## 9.2 submission_answers

```sql
submission_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,

    answer_text LONGTEXT,
    selected_option VARCHAR(255),

    score DECIMAL(8,2) NOT NULL DEFAULT 0,
    auto_saved_at DATETIME,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_submission_answers_submission FOREIGN KEY (submission_id) REFERENCES submissions(id),
    CONSTRAINT fk_submission_answers_question FOREIGN KEY (question_id) REFERENCES questions(id)
)
```

# 10. Grading Domain

## 10.1 grading_records

```sql
grading_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,

    auto_score DECIMAL(8,2),
    manual_score DECIMAL(8,2),
    final_score DECIMAL(8,2) NOT NULL DEFAULT 0,

    grader_id BIGINT,

    ai_suggestion_score DECIMAL(8,2),
    ai_comment TEXT,
    teacher_comment TEXT,

    graded_at DATETIME,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_grading_records_submission FOREIGN KEY (submission_id) REFERENCES submissions(id),
    CONSTRAINT fk_grading_records_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT fk_grading_records_grader FOREIGN KEY (grader_id) REFERENCES users(id)
)
```

# 11. Anti-Cheat Domain

## 11.1 anti_cheat_events

```sql
anti_cheat_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,

    event_type VARCHAR(64) NOT NULL,
    event_level VARCHAR(32) NOT NULL,

    event_data JSON,
    client_time DATETIME,

    created_at DATETIME NOT NULL,

    CONSTRAINT fk_anti_cheat_events_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_anti_cheat_events_exam FOREIGN KEY (exam_id) REFERENCES exams(id)
)
```

Event Types:

- browser_blur
- tab_hidden
- fullscreen_exit
- copy_attempt
- paste_attempt
- page_refresh
- abnormal_disconnect
- repeated_submit

# 12. Logging Domain

## 12.1 system_logs

```sql
system_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_type VARCHAR(64) NOT NULL,

    user_id BIGINT NULL,

    action VARCHAR(128) NOT NULL,
    message TEXT,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),

    created_at DATETIME NOT NULL,

    CONSTRAINT fk_system_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
)
```

## 12.2 ai_logs

```sql
ai_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    paper_id BIGINT NULL,
    parse_job_id BIGINT NULL,

    request TEXT,
    response LONGTEXT,
    model VARCHAR(64),
    provider VARCHAR(64),

    created_at DATETIME NOT NULL,

    CONSTRAINT fk_ai_logs_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
    CONSTRAINT fk_ai_logs_parse_job FOREIGN KEY (parse_job_id) REFERENCES ai_parse_jobs(id)
)
```

# 13. Statistics Domain

This table stores cached analytics.

## 13.1 exam_statistics

```sql
exam_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,

    average_score DECIMAL(8,2),
    max_score DECIMAL(8,2),
    min_score DECIMAL(8,2),

    pass_rate DECIMAL(5,2),
    submitted_count INT NOT NULL DEFAULT 0,
    participant_count INT NOT NULL DEFAULT 0,

    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_exam_statistics_exam FOREIGN KEY (exam_id) REFERENCES exams(id)
)
```

# 14. Relationship Summary

Core relationships:

users -> exams

users -> submissions

papers -> questions

papers -> ai_parse_jobs

ai_parse_jobs -> ai_parsed_questions

exams -> exam_questions

exams -> submissions

submissions -> submission_answers

questions -> question_options

questions -> question_answers

submissions -> grading_records

users -> anti_cheat_events

# 15. Data Lifecycle Rules

Paper

Immutable after upload.

Questions

Can evolve and are versioned.

Exams

Immutable after publish except status changes.

Submissions

Immutable after final submit.

Grading

Append-only history preferred.

Anti-cheat logs

Append-only only.

# 16. AI Data Separation Rule

AI output must never directly modify:

- questions
- exams
- grading_records

AI can only write through backend-controlled flows to:

- ai_parse_jobs
- ai_parsed_questions
- ai_logs

Final data must pass validation layer and teacher review.

# 17. Performance Considerations

Indexes required:

users.username

users.role

papers.file_hash

papers.parse_status

exams.status

exams.creator_id

exam_participants.exam_id

exam_participants.user_id

submissions.exam_id

submissions.user_id

anti_cheat_events.user_id

anti_cheat_events.exam_id

grading_records.submission_id

# 18. Final Rule

This database schema is the single source of truth for all persisted business data.

Any discrepancy between code and this document must be resolved by updating the code or formally updating this document.
