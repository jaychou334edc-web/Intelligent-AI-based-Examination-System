CREATE TABLE IF NOT EXISTS courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NULL,
    description TEXT NULL,
    teacher_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS teaching_classes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    grade VARCHAR(64) NULL,
    major VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_teaching_classes_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE IF NOT EXISTS class_students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_class_students_class FOREIGN KEY (class_id) REFERENCES teaching_classes(id),
    CONSTRAINT fk_class_students_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT uk_class_students_student UNIQUE (class_id, student_id)
);

ALTER TABLE exams ADD COLUMN course_id BIGINT NULL;
ALTER TABLE exams ADD COLUMN class_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_courses_teacher ON courses(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teaching_classes_course ON teaching_classes(course_id);
CREATE INDEX IF NOT EXISTS idx_class_students_class ON class_students(class_id);
CREATE INDEX IF NOT EXISTS idx_class_students_student ON class_students(student_id);
CREATE INDEX IF NOT EXISTS idx_exams_course ON exams(course_id);
CREATE INDEX IF NOT EXISTS idx_exams_class ON exams(class_id);
