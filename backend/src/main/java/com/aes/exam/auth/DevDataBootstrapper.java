package com.aes.exam.auth;

import com.aes.exam.auth.repository.UserRepository;
import com.aes.exam.auth.service.PasswordService;
import com.aes.exam.common.config.AesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
public class DevDataBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataBootstrapper.class);

    private final AesProperties properties;
    private final PasswordService passwordService;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public DevDataBootstrapper(
        AesProperties properties,
        PasswordService passwordService,
        UserRepository userRepository,
        JdbcTemplate jdbcTemplate
    ) {
        this.properties = properties;
        this.passwordService = passwordService;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.getAuth().getDevSeed().isEnabled()) {
            return;
        }

        seedUser("teacher", "Teacher@123456", UserRole.TEACHER, "测试教师");
        seedUser("student", "Student@123456", UserRole.STUDENT, "测试学生");
        seedAcademicData();
    }

    private void seedUser(String username, String password, UserRole role, String realName) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.info("Development user already exists: {}", username);
            return;
        }

        userRepository.createUser(username, passwordService.hash(password), role, realName);
        log.info("Development user created: {}", username);
    }

    private void seedAcademicData() {
        Long teacherId = jdbcTemplate.query("SELECT id FROM users WHERE username = 'teacher'",
            rs -> rs.next() ? rs.getLong("id") : null);
        Long studentId = jdbcTemplate.query("SELECT id FROM users WHERE username = 'student'",
            rs -> rs.next() ? rs.getLong("id") : null);
        if (teacherId == null || studentId == null) {
            return;
        }

        Long courseId = jdbcTemplate.query("SELECT id FROM courses WHERE code = 'JAVA-2026' AND is_deleted = 0",
            rs -> rs.next() ? rs.getLong("id") : null);
        if (courseId == null) {
            jdbcTemplate.update("""
                INSERT INTO courses (name, code, description, teacher_id, status, created_at, updated_at)
                VALUES ('Java 程序设计', 'JAVA-2026', '本地演示课程数据', ?, 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, teacherId);
            courseId = jdbcTemplate.query("SELECT id FROM courses WHERE code = 'JAVA-2026' AND is_deleted = 0",
                rs -> rs.next() ? rs.getLong("id") : null);
        }
        if (courseId == null) {
            return;
        }

        Long classId = jdbcTemplate.query("SELECT id FROM teaching_classes WHERE course_id = ? AND name = '2023 软件 1 班' AND is_deleted = 0",
            rs -> rs.next() ? rs.getLong("id") : null,
            courseId);
        if (classId == null) {
            jdbcTemplate.update("""
                INSERT INTO teaching_classes (course_id, name, grade, major, status, created_at, updated_at)
                VALUES (?, '2023 软件 1 班', '2023', '软件工程', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, courseId);
            classId = jdbcTemplate.query("SELECT id FROM teaching_classes WHERE course_id = ? AND name = '2023 软件 1 班' AND is_deleted = 0",
                rs -> rs.next() ? rs.getLong("id") : null,
                courseId);
        }
        if (classId == null) {
            return;
        }

        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_students WHERE class_id = ? AND student_id = ?",
            Integer.class,
            classId,
            studentId
        );
        if (exists != null && exists > 0) {
            jdbcTemplate.update("UPDATE class_students SET is_deleted = 0 WHERE class_id = ? AND student_id = ?", classId, studentId);
        } else {
            jdbcTemplate.update("""
                INSERT INTO class_students (class_id, student_id, created_at, is_deleted)
                VALUES (?, ?, CURRENT_TIMESTAMP, 0)
                """, classId, studentId);
        }
    }
}
