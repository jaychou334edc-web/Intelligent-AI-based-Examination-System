ALTER TABLE exams ADD COLUMN start_time DATETIME NULL;
ALTER TABLE exams ADD COLUMN end_time DATETIME NULL;

CREATE INDEX IF NOT EXISTS idx_exams_start_time ON exams(start_time);
CREATE INDEX IF NOT EXISTS idx_exams_end_time ON exams(end_time);
