package com.aes.exam.exam.repository;

import com.aes.exam.exam.dto.SaveAnswerRequest;
import com.aes.exam.exam.vo.ExamDetailVO;
import com.aes.exam.exam.vo.ExamQuestionVO;
import com.aes.exam.exam.vo.ExamSummaryVO;
import com.aes.exam.exam.vo.SaveAnswerVO;
import com.aes.exam.exam.vo.SubmitExamVO;
import com.aes.exam.question.vo.QuestionOptionVO;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcExamRepository implements ExamRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcExamRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long create(
        String title,
        String description,
        Integer durationMinutes,
        Long courseId,
        Long classId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long currentUserId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO exams (
                    title, description, duration_minutes, course_id, class_id, start_time, end_time,
                    status, created_at, updated_at, created_by, updated_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 'draft', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
                """, new String[]{"id"});
            statement.setString(1, title);
            statement.setString(2, description);
            statement.setInt(3, durationMinutes);
            statement.setObject(4, courseId);
            statement.setObject(5, classId);
            statement.setObject(6, startTime);
            statement.setObject(7, endTime);
            statement.setLong(8, currentUserId);
            statement.setLong(9, currentUserId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public void updateDraft(
        Long examId,
        String title,
        String description,
        Integer durationMinutes,
        Long courseId,
        Long classId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long teacherId
    ) {
        jdbcTemplate.update("""
            UPDATE exams
            SET title = ?, description = ?, duration_minutes = ?, course_id = ?, class_id = ?,
                start_time = ?, end_time = ?,
                updated_at = CURRENT_TIMESTAMP, updated_by = ?
            WHERE id = ? AND created_by = ? AND status = 'draft' AND is_deleted = 0
            """, title, description, durationMinutes, courseId, classId, startTime, endTime, teacherId, examId, teacherId);
    }

    @Override
    public List<ExamSummaryVO> findTeacherExams(Long teacherId) {
        return jdbcTemplate.query("""
                SELECT e.id, e.title, e.description, e.duration_minutes, e.course_id, e.class_id,
                       e.start_time, e.end_time,
                       c.name AS course_name, tc.name AS class_name, e.status, e.published_at, e.created_at,
                       COUNT(eq.id) AS question_count, COALESCE(SUM(eq.score), 0) AS total_score,
                       NULL AS submission_status
                FROM exams e
                LEFT JOIN courses c ON c.id = e.course_id
                LEFT JOIN teaching_classes tc ON tc.id = e.class_id
                LEFT JOIN exam_questions eq ON eq.exam_id = e.id
                WHERE e.created_by = ? AND e.is_deleted = 0
                GROUP BY e.id, e.title, e.description, e.duration_minutes, e.course_id, e.class_id,
                         e.start_time, e.end_time,
                         c.name, tc.name, e.status, e.published_at, e.created_at
                ORDER BY e.created_at DESC, e.id DESC
                """, this::mapSummary, teacherId);
    }

    @Override
    public Optional<ExamDetailVO> findTeacherExamDetail(Long examId, Long teacherId) {
        return findExamDetail("""
            SELECT e.id, e.title, e.description, e.duration_minutes, e.course_id, e.class_id,
                   e.start_time, e.end_time,
                   c.name AS course_name, tc.name AS class_name, e.status, e.published_at,
                   COUNT(eq.id) AS question_count, COALESCE(SUM(eq.score), 0) AS total_score,
                   NULL AS submission_id, NULL AS submission_status, NULL AS submission_started_at
            FROM exams e
            LEFT JOIN courses c ON c.id = e.course_id
            LEFT JOIN teaching_classes tc ON tc.id = e.class_id
            LEFT JOIN exam_questions eq ON eq.exam_id = e.id
            WHERE e.id = ? AND e.created_by = ? AND e.is_deleted = 0
            GROUP BY e.id, e.title, e.description, e.duration_minutes, e.course_id, e.class_id,
                     e.start_time, e.end_time,
                     c.name, tc.name, e.status, e.published_at
            """, examId, teacherId);
    }

    @Override
    public boolean isDraftOwnedByTeacher(Long examId, Long teacherId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM exams
            WHERE id = ? AND created_by = ? AND status = 'draft' AND is_deleted = 0
            """, Integer.class, examId, teacherId);
        return count != null && count > 0;
    }

    @Override
    public void replaceQuestions(Long examId, List<Long> questionIds) {
        jdbcTemplate.update("DELETE FROM exam_questions WHERE exam_id = ?", examId);
        for (int index = 0; index < questionIds.size(); index++) {
            Long questionId = questionIds.get(index);
            jdbcTemplate.update("""
                INSERT INTO exam_questions (exam_id, question_id, sort_order, score, created_at)
                SELECT ?, q.id, ?, q.score, CURRENT_TIMESTAMP
                FROM questions q
                WHERE q.id = ? AND q.is_deleted = 0 AND q.status = 'active'
                """, examId, index + 1, questionId);
        }
    }

    @Override
    public void publish(Long examId, Long teacherId) {
        jdbcTemplate.update("""
            UPDATE exams
            SET status = 'published', published_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP, updated_by = ?
            WHERE id = ? AND created_by = ? AND status = 'draft' AND is_deleted = 0
            """, teacherId, examId, teacherId);
    }

    @Override
    public void deleteDraft(Long examId, Long teacherId) {
        jdbcTemplate.update("""
            DELETE FROM exam_questions
            WHERE exam_id = ? AND EXISTS (
                SELECT 1 FROM exams
                WHERE id = ? AND created_by = ? AND status = 'draft'
            )
            """, examId, examId, teacherId);
        jdbcTemplate.update("""
            UPDATE exams
            SET is_deleted = 1, updated_at = CURRENT_TIMESTAMP, updated_by = ?
            WHERE id = ? AND created_by = ? AND status = 'draft' AND is_deleted = 0
            """, teacherId, examId, teacherId);
    }

    @Override
    public void archivePublished(Long examId, Long teacherId) {
        jdbcTemplate.update("""
            UPDATE exams
            SET status = 'archived', updated_at = CURRENT_TIMESTAMP, updated_by = ?
            WHERE id = ? AND created_by = ? AND status = 'published' AND is_deleted = 0
            """, teacherId, examId, teacherId);
    }

    @Override
    public void assignPublishedExamToStudents(Long examId, Long classId) {
        if (classId == null) {
            jdbcTemplate.update("""
                INSERT INTO exam_participants (exam_id, student_id, status, assigned_at)
                SELECT ?, u.id, 'assigned', CURRENT_TIMESTAMP
                FROM users u
                WHERE u.role = 'student' AND u.status = 'active' AND u.is_deleted = 0
                  AND NOT EXISTS (
                      SELECT 1 FROM exam_participants ep
                      WHERE ep.exam_id = ? AND ep.student_id = u.id
                  )
                """, examId, examId);
            return;
        }

        jdbcTemplate.update("""
            INSERT INTO exam_participants (exam_id, student_id, status, assigned_at)
            SELECT ?, u.id, 'assigned', CURRENT_TIMESTAMP
            FROM class_students cs
            JOIN users u ON u.id = cs.student_id
            WHERE cs.class_id = ? AND cs.is_deleted = 0
              AND u.role = 'student' AND u.status = 'active' AND u.is_deleted = 0
              AND NOT EXISTS (
                  SELECT 1 FROM exam_participants ep
                  WHERE ep.exam_id = ? AND ep.student_id = u.id
              )
            """, examId, classId, examId);
    }

    @Override
    public List<ExamSummaryVO> findStudentExams(Long studentId) {
        return jdbcTemplate.query("""
                SELECT e.id, e.title, e.description, e.duration_minutes, e.course_id, e.class_id,
                       e.start_time, e.end_time,
                       c.name AS course_name, tc.name AS class_name, e.status, e.published_at, e.created_at,
                       COUNT(eq.id) AS question_count, COALESCE(SUM(eq.score), 0) AS total_score,
                       COALESCE(s.status, ep.status) AS submission_status
                FROM exam_participants ep
                JOIN exams e ON e.id = ep.exam_id
                LEFT JOIN courses c ON c.id = e.course_id
                LEFT JOIN teaching_classes tc ON tc.id = e.class_id
                LEFT JOIN exam_questions eq ON eq.exam_id = e.id
                LEFT JOIN submissions s ON s.exam_id = e.id AND s.student_id = ep.student_id
                WHERE ep.student_id = ? AND e.status = 'published' AND e.is_deleted = 0
                GROUP BY e.id, e.title, e.description, e.duration_minutes, e.course_id, e.class_id,
                         e.start_time, e.end_time,
                         c.name, tc.name, e.status, e.published_at, e.created_at,
                         s.status, ep.status
                ORDER BY e.published_at DESC, e.id DESC
                """, this::mapSummary, studentId);
    }

    @Override
    public Optional<ExamDetailVO> findStudentExamDetail(Long examId, Long studentId) {
        return findExamDetail("""
            SELECT e.id, e.title, e.description, e.duration_minutes, e.course_id, e.class_id,
                   e.start_time, e.end_time,
                   c.name AS course_name, tc.name AS class_name, e.status, e.published_at,
                   COUNT(eq.id) AS question_count, COALESCE(SUM(eq.score), 0) AS total_score,
                   s.id AS submission_id, COALESCE(s.status, ep.status) AS submission_status, s.started_at AS submission_started_at
            FROM exam_participants ep
            JOIN exams e ON e.id = ep.exam_id
            LEFT JOIN courses c ON c.id = e.course_id
            LEFT JOIN teaching_classes tc ON tc.id = e.class_id
            LEFT JOIN exam_questions eq ON eq.exam_id = e.id
            LEFT JOIN submissions s ON s.exam_id = e.id AND s.student_id = ep.student_id
            WHERE e.id = ? AND ep.student_id = ? AND e.status = 'published' AND e.is_deleted = 0
            GROUP BY e.id, e.title, e.description, e.duration_minutes, e.course_id, e.class_id,
                     e.start_time, e.end_time,
                     c.name, tc.name, e.status, e.published_at, s.id, s.status, ep.status
            """, examId, studentId);
    }

    @Override
    public Long ensureSubmission(Long examId, Long studentId) {
        Long existingId = jdbcTemplate.query("""
                SELECT id FROM submissions WHERE exam_id = ? AND student_id = ?
                """,
            rs -> rs.next() ? rs.getLong("id") : null,
            examId,
            studentId
        );
        if (existingId != null) {
            return existingId;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO submissions (exam_id, student_id, status, started_at, created_at, updated_at)
                VALUES (?, ?, 'in_progress', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, new String[]{"id"});
            statement.setLong(1, examId);
            statement.setLong(2, studentId);
            return statement;
        }, keyHolder);
        jdbcTemplate.update("""
            UPDATE exam_participants
            SET status = 'in_progress', started_at = COALESCE(started_at, CURRENT_TIMESTAMP)
            WHERE exam_id = ? AND student_id = ? AND status <> 'submitted'
            """, examId, studentId);
        return keyHolder.getKey().longValue();
    }

    @Override
    public boolean isQuestionInExam(Long examId, Long questionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM exam_questions WHERE exam_id = ? AND question_id = ?",
            Integer.class,
            examId,
            questionId
        );
        return count != null && count > 0;
    }

    @Override
    public SaveAnswerVO saveAnswer(Long submissionId, Long examId, SaveAnswerRequest request) {
        jdbcTemplate.update("""
            INSERT INTO submission_answers (submission_id, question_id, answer_text, is_correct, score, updated_at)
            VALUES (?, ?, ?, NULL, NULL, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE answer_text = VALUES(answer_text), updated_at = CURRENT_TIMESTAMP
            """, submissionId, request.questionId(), request.answer());
        jdbcTemplate.update("UPDATE submissions SET updated_at = CURRENT_TIMESTAMP WHERE id = ?", submissionId);
        return new SaveAnswerVO(submissionId, request.questionId(), "saved", LocalDateTime.now());
    }

    @Override
    public SubmitExamVO submit(Long submissionId, Long examId, List<SaveAnswerRequest> answers) {
        if (answers != null) {
            for (SaveAnswerRequest answer : answers) {
                saveAnswer(submissionId, examId, answer);
            }
        }
        jdbcTemplate.update("""
            UPDATE submissions
            SET status = 'submitted', submitted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status <> 'submitted'
            """, submissionId);
        SubmissionParticipant participant = jdbcTemplate.queryForObject("""
            SELECT exam_id, student_id, submitted_at FROM submissions WHERE id = ?
            """, (rs, rowNum) -> new SubmissionParticipant(
                rs.getLong("exam_id"),
                rs.getLong("student_id"),
                toLocalDateTime(rs.getTimestamp("submitted_at"))
            ), submissionId);
        if (participant != null) {
            jdbcTemplate.update("""
                UPDATE exam_participants
                SET status = 'submitted', submitted_at = ?
                WHERE exam_id = ? AND student_id = ?
                """, participant.submittedAt(), participant.examId(), participant.studentId());
        }

        return findSubmitResult(submissionId);
    }

    @Override
    public SubmitExamVO findSubmitResult(Long submissionId) {
        return jdbcTemplate.queryForObject("""
            SELECT id, status, total_score, submitted_at FROM submissions WHERE id = ?
            """, (rs, rowNum) -> new SubmitExamVO(
            rs.getLong("id"),
            rs.getString("status"),
            rs.getBigDecimal("total_score"),
            toLocalDateTime(rs.getTimestamp("submitted_at"))
        ), submissionId);
    }

    private Optional<ExamDetailVO> findExamDetail(String sql, Object... args) {
        List<ExamDetailVO> exams = jdbcTemplate.query(sql, (rs, rowNum) -> mapDetailWithoutQuestions(rs), args);
        if (exams.isEmpty()) {
            return Optional.empty();
        }
        ExamDetailVO exam = exams.get(0);
        boolean includeAnswer = exam.submissionStatus() == null;
        return Optional.of(new ExamDetailVO(
            exam.id(),
            exam.title(),
            exam.description(),
            exam.durationMinutes(),
            exam.courseId(),
            exam.classId(),
            exam.courseName(),
            exam.className(),
            exam.status(),
            exam.questionCount(),
            exam.totalScore(),
            exam.publishedAt(),
            exam.startTime(),
            exam.endTime(),
            findExamQuestions(exam.id(), exam.submissionId(), includeAnswer),
            exam.submissionId(),
            exam.submissionStatus(),
            exam.submissionStartedAt(),
            remainingSeconds(exam.durationMinutes(), exam.submissionStartedAt(), exam.submissionStatus(), exam.endTime())
        ));
    }

    private List<ExamQuestionVO> findExamQuestions(Long examId, Long submissionId, boolean includeAnswer) {
        List<ExamQuestionVO> questions = jdbcTemplate.query("""
                SELECT q.id, q.source_paper_id, q.question_type, q.stem, q.analysis, eq.score, qa.answer_text AS correct_answer,
                       eq.sort_order, sa.answer_text AS saved_answer
                FROM exam_questions eq
                JOIN questions q ON q.id = eq.question_id
                LEFT JOIN question_answers qa ON qa.question_id = q.id
                LEFT JOIN submission_answers sa ON sa.question_id = q.id AND sa.submission_id = ?
                WHERE eq.exam_id = ?
                ORDER BY eq.sort_order ASC, eq.id ASC
                """,
            (rs, rowNum) -> new ExamQuestionVO(
                rs.getLong("id"),
                (Long) rs.getObject("source_paper_id"),
                rs.getString("question_type"),
                rs.getString("stem"),
                rs.getString("analysis"),
                rs.getBigDecimal("score"),
                includeAnswer ? rs.getString("correct_answer") : null,
                findOptions(rs.getLong("id")),
                rs.getInt("sort_order"),
                rs.getString("saved_answer")
            ),
            submissionId,
            examId
        );
        return questions;
    }

    private List<QuestionOptionVO> findOptions(Long questionId) {
        return jdbcTemplate.query("""
                SELECT option_key, option_text
                FROM question_options
                WHERE question_id = ?
                ORDER BY sort_order ASC, id ASC
                """,
            (rs, rowNum) -> new QuestionOptionVO(rs.getString("option_key"), rs.getString("option_text")),
            questionId
        );
    }

    private ExamSummaryVO mapSummary(ResultSet rs, int rowNum) throws SQLException {
        return new ExamSummaryVO(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getInt("duration_minutes"),
            (Long) rs.getObject("course_id"),
            (Long) rs.getObject("class_id"),
            rs.getString("course_name"),
            rs.getString("class_name"),
            rs.getString("status"),
            rs.getInt("question_count"),
            rs.getBigDecimal("total_score"),
            toLocalDateTime(rs.getTimestamp("published_at")),
            toLocalDateTime(rs.getTimestamp("start_time")),
            toLocalDateTime(rs.getTimestamp("end_time")),
            toLocalDateTime(rs.getTimestamp("created_at")),
            rs.getString("submission_status")
        );
    }

    private ExamDetailVO mapDetailWithoutQuestions(ResultSet rs) throws SQLException {
        Long submissionId = (Long) rs.getObject("submission_id");
        return new ExamDetailVO(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getInt("duration_minutes"),
            (Long) rs.getObject("course_id"),
            (Long) rs.getObject("class_id"),
            rs.getString("course_name"),
            rs.getString("class_name"),
            rs.getString("status"),
            rs.getInt("question_count"),
            rs.getBigDecimal("total_score"),
            toLocalDateTime(rs.getTimestamp("published_at")),
            toLocalDateTime(rs.getTimestamp("start_time")),
            toLocalDateTime(rs.getTimestamp("end_time")),
            List.of(),
            submissionId,
            rs.getString("submission_status"),
            toLocalDateTime(rs.getTimestamp("submission_started_at")),
            null
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long remainingSeconds(Integer durationMinutes, LocalDateTime startedAt, String submissionStatus, LocalDateTime examEndTime) {
        if (durationMinutes == null || startedAt == null || "submitted".equals(submissionStatus)) {
            return null;
        }
        LocalDateTime endsAt = startedAt.plusMinutes(durationMinutes);
        if (examEndTime != null && examEndTime.isBefore(endsAt)) {
            endsAt = examEndTime;
        }
        long seconds = Duration.between(LocalDateTime.now(), endsAt).getSeconds();
        return Math.max(0L, seconds);
    }

    private record SubmissionParticipant(Long examId, Long studentId, LocalDateTime submittedAt) {
    }
}
