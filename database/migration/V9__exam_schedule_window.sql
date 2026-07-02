ALTER TABLE exams ADD COLUMN start_time DATETIME NULL;
ALTER TABLE exams ADD COLUMN end_time DATETIME NULL;
ALTER TABLE exams ADD INDEX idx_exams_start_time (start_time);
ALTER TABLE exams ADD INDEX idx_exams_end_time (end_time);
