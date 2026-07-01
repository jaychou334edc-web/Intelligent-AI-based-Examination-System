package com.aes.exam.academic.repository;

import com.aes.exam.academic.dto.CreateCourseRequest;
import com.aes.exam.academic.dto.CreateTeachingClassRequest;
import com.aes.exam.academic.vo.ClassStudentVO;
import com.aes.exam.academic.vo.CourseVO;
import com.aes.exam.academic.vo.TeachingClassVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAcademicRepository implements AcademicRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAcademicRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long createCourse(CreateCourseRequest request, Long currentUserId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO courses (name, code, description, teacher_id, status, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
                """, new String[]{"id"});
            statement.setString(1, request.name().trim());
            statement.setString(2, blankToNull(request.code()));
            statement.setString(3, blankToNull(request.description()));
            statement.setObject(4, request.teacherId());
            statement.setLong(5, currentUserId);
            statement.setLong(6, currentUserId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public Long createClass(CreateTeachingClassRequest request, Long currentUserId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO teaching_classes (course_id, name, grade, major, status, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
                """, new String[]{"id"});
            statement.setLong(1, request.courseId());
            statement.setString(2, request.name().trim());
            statement.setString(3, blankToNull(request.grade()));
            statement.setString(4, blankToNull(request.major()));
            statement.setLong(5, currentUserId);
            statement.setLong(6, currentUserId);
            return statement;
        }, keyHolder);
        Long classId = keyHolder.getKey().longValue();
        replaceClassStudents(classId, request.studentIds(), currentUserId);
        return classId;
    }

    @Override
    public List<CourseVO> findCourses() {
        return jdbcTemplate.query("""
                SELECT c.id, c.name, c.code, c.description, c.teacher_id, up.real_name AS teacher_name, c.status,
                       COUNT(DISTINCT tc.id) AS class_count,
                       COUNT(DISTINCT cs.student_id) AS student_count,
                       c.created_at
                FROM courses c
                LEFT JOIN user_profiles up ON up.user_id = c.teacher_id AND up.is_deleted = 0
                LEFT JOIN teaching_classes tc ON tc.course_id = c.id AND tc.is_deleted = 0
                LEFT JOIN class_students cs ON cs.class_id = tc.id AND cs.is_deleted = 0
                WHERE c.is_deleted = 0
                GROUP BY c.id, c.name, c.code, c.description, c.teacher_id, up.real_name, c.status, c.created_at
                ORDER BY c.created_at DESC, c.id DESC
                """,
            this::mapCourse);
    }

    @Override
    public List<TeachingClassVO> findClasses() {
        return jdbcTemplate.query("""
                SELECT tc.id, tc.course_id, c.name AS course_name, tc.name, tc.grade, tc.major, tc.status,
                       COUNT(cs.student_id) AS student_count, tc.created_at
                FROM teaching_classes tc
                JOIN courses c ON c.id = tc.course_id
                LEFT JOIN class_students cs ON cs.class_id = tc.id AND cs.is_deleted = 0
                WHERE tc.is_deleted = 0 AND c.is_deleted = 0
                GROUP BY tc.id, tc.course_id, c.name, tc.name, tc.grade, tc.major, tc.status, tc.created_at
                ORDER BY tc.created_at DESC, tc.id DESC
                """,
            this::mapClass);
    }

    @Override
    public List<ClassStudentVO> findAvailableStudents() {
        return jdbcTemplate.query("""
                SELECT u.id, u.username, up.real_name, up.student_number
                FROM users u
                LEFT JOIN user_profiles up ON up.user_id = u.id AND up.is_deleted = 0
                WHERE u.role = 'student' AND u.status = 'active' AND u.is_deleted = 0
                ORDER BY u.username ASC
                """,
            this::mapStudent);
    }

    @Override
    public List<ClassStudentVO> findStudentsByClass(Long classId) {
        return jdbcTemplate.query("""
                SELECT u.id, u.username, up.real_name, up.student_number
                FROM class_students cs
                JOIN users u ON u.id = cs.student_id
                LEFT JOIN user_profiles up ON up.user_id = u.id AND up.is_deleted = 0
                WHERE cs.class_id = ? AND cs.is_deleted = 0 AND u.is_deleted = 0
                ORDER BY u.username ASC
                """,
            this::mapStudent,
            classId);
    }

    @Override
    public Optional<TeachingClassVO> findClassById(Long classId) {
        return jdbcTemplate.query("""
                SELECT tc.id, tc.course_id, c.name AS course_name, tc.name, tc.grade, tc.major, tc.status,
                       COUNT(cs.student_id) AS student_count, tc.created_at
                FROM teaching_classes tc
                JOIN courses c ON c.id = tc.course_id
                LEFT JOIN class_students cs ON cs.class_id = tc.id AND cs.is_deleted = 0
                WHERE tc.id = ? AND tc.is_deleted = 0 AND c.is_deleted = 0
                GROUP BY tc.id, tc.course_id, c.name, tc.name, tc.grade, tc.major, tc.status, tc.created_at
                """,
            this::mapClass,
            classId
        ).stream().findFirst();
    }

    @Override
    public void replaceClassStudents(Long classId, List<Long> studentIds, Long currentUserId) {
        jdbcTemplate.update("UPDATE class_students SET is_deleted = 1 WHERE class_id = ?", classId);
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }
        for (Long studentId : studentIds.stream().distinct().toList()) {
            Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM class_students WHERE class_id = ? AND student_id = ?",
                Integer.class,
                classId,
                studentId
            );
            if (existing != null && existing > 0) {
                jdbcTemplate.update(
                    "UPDATE class_students SET is_deleted = 0 WHERE class_id = ? AND student_id = ?",
                    classId,
                    studentId
                );
            } else {
                jdbcTemplate.update("""
                    INSERT INTO class_students (class_id, student_id, created_at, created_by, is_deleted)
                    VALUES (?, ?, CURRENT_TIMESTAMP, ?, 0)
                    """, classId, studentId, currentUserId);
            }
        }
    }

    @Override
    public int countClassStudents(Long classId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_students WHERE class_id = ? AND is_deleted = 0",
            Integer.class,
            classId
        );
        return count == null ? 0 : count;
    }

    private CourseVO mapCourse(ResultSet rs, int rowNum) throws SQLException {
        return new CourseVO(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("code"),
            rs.getString("description"),
            (Long) rs.getObject("teacher_id"),
            rs.getString("teacher_name"),
            rs.getString("status"),
            rs.getInt("class_count"),
            rs.getInt("student_count"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private TeachingClassVO mapClass(ResultSet rs, int rowNum) throws SQLException {
        return new TeachingClassVO(
            rs.getLong("id"),
            rs.getLong("course_id"),
            rs.getString("course_name"),
            rs.getString("name"),
            rs.getString("grade"),
            rs.getString("major"),
            rs.getString("status"),
            rs.getInt("student_count"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private ClassStudentVO mapStudent(ResultSet rs, int rowNum) throws SQLException {
        return new ClassStudentVO(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("real_name"),
            rs.getString("student_number")
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
