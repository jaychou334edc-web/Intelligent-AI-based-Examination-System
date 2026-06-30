package com.aes.exam.grading.repository;

import com.aes.exam.common.config.AesProperties;
import com.aes.exam.grading.vo.GradingAnswerVO;
import com.aes.exam.grading.vo.SubmissionGradingVO;
import com.aes.exam.grading.vo.SubmissionSummaryVO;
import com.aes.exam.question.vo.QuestionOptionVO;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcGradingRepository implements GradingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AesProperties aesProperties;

    public JdbcGradingRepository(JdbcTemplate jdbcTemplate, AesProperties aesProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.aesProperties = aesProperties;
    }

    @Override
    public void gradeSubmittedObjectiveAnswers(Long submissionId) {
        List<AnswerForGrading> answers = jdbcTemplate.query("""
                SELECT s.id AS submission_id, q.id AS question_id, q.question_type, eq.score AS max_score,
                       qa.answer_text AS correct_answer, qa.match_rule, sa.answer_text AS student_answer
                FROM submissions s
                JOIN exam_questions eq ON eq.exam_id = s.exam_id
                JOIN questions q ON q.id = eq.question_id
                LEFT JOIN question_answers qa ON qa.question_id = q.id
                LEFT JOIN submission_answers sa ON sa.submission_id = s.id AND sa.question_id = q.id
                WHERE s.id = ?
                ORDER BY eq.sort_order ASC
                """, (rs, rowNum) -> new AnswerForGrading(
                    rs.getLong("submission_id"),
                    rs.getLong("question_id"),
                    rs.getString("question_type"),
                    rs.getBigDecimal("max_score"),
                    rs.getString("correct_answer"),
                    rs.getString("match_rule"),
                    rs.getString("student_answer")
                ), submissionId);

        for (AnswerForGrading answer : answers) {
            if (isAutoGradable(answer.questionType())) {
                boolean correct = isAnswerCorrect(answer);
                BigDecimal score = correct ? answer.maxScore() : BigDecimal.ZERO;
                upsertSubmissionAnswer(answer.submissionId(), answer.questionId(), answer.studentAnswer(), correct, score);
                upsertGradingRecord(answer.submissionId(), answer.questionId(), score, null, score, null, "auto_graded");
            } else {
                upsertSubmissionAnswer(answer.submissionId(), answer.questionId(), answer.studentAnswer(), null, null);
                upsertGradingRecord(answer.submissionId(), answer.questionId(), null, null, BigDecimal.ZERO, null, "pending");
            }
        }
        refreshSubmissionTotal(submissionId);
    }

    @Override
    public List<SubmissionSummaryVO> findTeacherSubmissions(Long teacherId) {
        return jdbcTemplate.query("""
                SELECT s.id AS submission_id, e.id AS exam_id, e.title AS exam_title, u.id AS student_id,
                       COALESCE(up.real_name, u.username) AS student_name, s.status AS submission_status,
                       s.total_score, COALESCE(SUM(eq.score), 0) AS max_score, s.submitted_at, s.graded_at
                FROM submissions s
                JOIN exams e ON e.id = s.exam_id
                JOIN users u ON u.id = s.student_id
                LEFT JOIN user_profiles up ON up.user_id = u.id
                LEFT JOIN exam_questions eq ON eq.exam_id = e.id
                WHERE e.created_by = ? AND s.status = 'submitted' AND e.is_deleted = 0
                GROUP BY s.id, e.id, e.title, u.id, up.real_name, u.username, s.status, s.total_score, s.submitted_at, s.graded_at
                ORDER BY s.submitted_at DESC, s.id DESC
                """, this::mapSummary, teacherId);
    }

    @Override
    public List<SubmissionSummaryVO> findStudentSubmissions(Long studentId) {
        return jdbcTemplate.query("""
                SELECT s.id AS submission_id, e.id AS exam_id, e.title AS exam_title, u.id AS student_id,
                       COALESCE(up.real_name, u.username) AS student_name, s.status AS submission_status,
                       s.total_score, COALESCE(SUM(eq.score), 0) AS max_score, s.submitted_at, s.graded_at
                FROM submissions s
                JOIN exams e ON e.id = s.exam_id
                JOIN users u ON u.id = s.student_id
                LEFT JOIN user_profiles up ON up.user_id = u.id
                LEFT JOIN exam_questions eq ON eq.exam_id = e.id
                WHERE s.student_id = ? AND s.status = 'submitted' AND e.is_deleted = 0
                GROUP BY s.id, e.id, e.title, u.id, up.real_name, u.username, s.status, s.total_score, s.submitted_at, s.graded_at
                ORDER BY s.submitted_at DESC, s.id DESC
                """, this::mapSummary, studentId);
    }

    @Override
    public Optional<SubmissionGradingVO> findTeacherSubmission(Long submissionId, Long teacherId) {
        return findSubmission("""
            SELECT s.id AS submission_id, e.id AS exam_id, e.title AS exam_title, u.id AS student_id,
                   COALESCE(up.real_name, u.username) AS student_name, s.status AS submission_status,
                   s.total_score, COALESCE(SUM(eq.score), 0) AS max_score, s.submitted_at, s.graded_at
            FROM submissions s
            JOIN exams e ON e.id = s.exam_id
            JOIN users u ON u.id = s.student_id
            LEFT JOIN user_profiles up ON up.user_id = u.id
            LEFT JOIN exam_questions eq ON eq.exam_id = e.id
            WHERE s.id = ? AND e.created_by = ? AND e.is_deleted = 0
            GROUP BY s.id, e.id, e.title, u.id, up.real_name, u.username, s.status, s.total_score, s.submitted_at, s.graded_at
            """, submissionId, teacherId);
    }

    @Override
    public Optional<SubmissionGradingVO> findStudentSubmission(Long submissionId, Long studentId) {
        return findSubmission("""
            SELECT s.id AS submission_id, e.id AS exam_id, e.title AS exam_title, u.id AS student_id,
                   COALESCE(up.real_name, u.username) AS student_name, s.status AS submission_status,
                   s.total_score, COALESCE(SUM(eq.score), 0) AS max_score, s.submitted_at, s.graded_at
            FROM submissions s
            JOIN exams e ON e.id = s.exam_id
            JOIN users u ON u.id = s.student_id
            LEFT JOIN user_profiles up ON up.user_id = u.id
            LEFT JOIN exam_questions eq ON eq.exam_id = e.id
            WHERE s.id = ? AND s.student_id = ? AND s.status = 'submitted' AND e.is_deleted = 0
            GROUP BY s.id, e.id, e.title, u.id, up.real_name, u.username, s.status, s.total_score, s.submitted_at, s.graded_at
            """, submissionId, studentId);
    }

    @Override
    public void updateManualGrade(Long submissionId, Long questionId, BigDecimal score, String teacherComment, Long graderId) {
        BigDecimal maxScore = jdbcTemplate.queryForObject("""
            SELECT eq.score
            FROM submissions s
            JOIN exam_questions eq ON eq.exam_id = s.exam_id
            WHERE s.id = ? AND eq.question_id = ?
            """, BigDecimal.class, submissionId, questionId);
        if (maxScore != null && score.compareTo(maxScore) > 0) {
            throw new IllegalArgumentException("人工评分不能超过题目分值");
        }
        jdbcTemplate.update("""
            UPDATE submission_answers
            SET score = ?, updated_at = CURRENT_TIMESTAMP
            WHERE submission_id = ? AND question_id = ?
            """, score, submissionId, questionId);
        jdbcTemplate.update("""
            INSERT INTO grading_records (
                submission_id, question_id, auto_score, manual_score, final_score, grader_id,
                teacher_comment, grading_status, graded_at, created_at, updated_at
            )
            VALUES (?, ?, NULL, ?, ?, ?, ?, 'manual_graded', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE manual_score = VALUES(manual_score), final_score = VALUES(final_score),
                grader_id = VALUES(grader_id), teacher_comment = VALUES(teacher_comment),
                grading_status = 'manual_graded', graded_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            """, submissionId, questionId, score, score, graderId, teacherComment);
        refreshSubmissionTotal(submissionId);
    }

    @Override
    public void refreshSubmissionTotal(Long submissionId) {
        jdbcTemplate.update("""
            UPDATE submissions
            SET total_score = (
                    SELECT COALESCE(SUM(final_score), 0)
                    FROM grading_records
                    WHERE submission_id = ?
                ),
                graded_at = CASE
                    WHEN NOT EXISTS (
                        SELECT 1 FROM grading_records
                        WHERE submission_id = ? AND grading_status = 'pending'
                    ) THEN CURRENT_TIMESTAMP
                    ELSE graded_at
                END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, submissionId, submissionId, submissionId);
    }

    private Optional<SubmissionGradingVO> findSubmission(String sql, Object... args) {
        List<SubmissionSummaryVO> summaries = jdbcTemplate.query(sql, this::mapSummary, args);
        if (summaries.isEmpty()) {
            return Optional.empty();
        }
        SubmissionSummaryVO summary = summaries.get(0);
        return Optional.of(new SubmissionGradingVO(
            summary.submissionId(),
            summary.examId(),
            summary.examTitle(),
            summary.studentId(),
            summary.studentName(),
            summary.submissionStatus(),
            summary.totalScore(),
            summary.maxScore(),
            summary.submittedAt(),
            summary.gradedAt(),
            findAnswers(summary.submissionId())
        ));
    }

    private List<GradingAnswerVO> findAnswers(Long submissionId) {
        return jdbcTemplate.query("""
                SELECT q.id AS question_id, q.source_paper_id, q.question_type, q.stem, eq.score AS max_score,
                       qa.answer_text AS correct_answer, sa.answer_text AS student_answer, sa.is_correct,
                       gr.auto_score, gr.manual_score, gr.final_score, gr.grading_status, gr.teacher_comment
                FROM submissions s
                JOIN exam_questions eq ON eq.exam_id = s.exam_id
                JOIN questions q ON q.id = eq.question_id
                LEFT JOIN question_answers qa ON qa.question_id = q.id
                LEFT JOIN submission_answers sa ON sa.submission_id = s.id AND sa.question_id = q.id
                LEFT JOIN grading_records gr ON gr.submission_id = s.id AND gr.question_id = q.id
                WHERE s.id = ?
                ORDER BY eq.sort_order ASC, eq.id ASC
                """, (rs, rowNum) -> new GradingAnswerVO(
                    rs.getLong("question_id"),
                    (Long) rs.getObject("source_paper_id"),
                    rs.getString("question_type"),
                    rs.getString("stem"),
                    findOptions(rs.getLong("question_id")),
                    rs.getBigDecimal("max_score"),
                    rs.getString("correct_answer"),
                    rs.getString("student_answer"),
                    toBoolean(rs.getObject("is_correct")),
                    rs.getBigDecimal("auto_score"),
                    rs.getBigDecimal("manual_score"),
                    rs.getBigDecimal("final_score"),
                    rs.getString("grading_status"),
                    rs.getString("teacher_comment")
                ), submissionId);
    }

    private List<QuestionOptionVO> findOptions(Long questionId) {
        return jdbcTemplate.query("""
                SELECT option_key, option_text
                FROM question_options
                WHERE question_id = ?
                ORDER BY sort_order ASC, id ASC
                """, (rs, rowNum) -> new QuestionOptionVO(rs.getString("option_key"), rs.getString("option_text")), questionId);
    }

    private void upsertSubmissionAnswer(Long submissionId, Long questionId, String answerText, Boolean isCorrect, BigDecimal score) {
        jdbcTemplate.update("""
            INSERT INTO submission_answers (submission_id, question_id, answer_text, is_correct, score, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE is_correct = VALUES(is_correct), score = VALUES(score), updated_at = CURRENT_TIMESTAMP
            """, submissionId, questionId, answerText, isCorrect, score);
    }

    private void upsertGradingRecord(
        Long submissionId,
        Long questionId,
        BigDecimal autoScore,
        BigDecimal manualScore,
        BigDecimal finalScore,
        String teacherComment,
        String gradingStatus
    ) {
        jdbcTemplate.update("""
            INSERT INTO grading_records (
                submission_id, question_id, auto_score, manual_score, final_score,
                teacher_comment, grading_status, graded_at, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, CASE WHEN ? = 'pending' THEN NULL ELSE CURRENT_TIMESTAMP END, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE auto_score = VALUES(auto_score), final_score = VALUES(final_score),
                grading_status = VALUES(grading_status), graded_at = VALUES(graded_at), updated_at = CURRENT_TIMESTAMP
            """, submissionId, questionId, autoScore, manualScore, finalScore, teacherComment, gradingStatus, gradingStatus);
    }

    private SubmissionSummaryVO mapSummary(ResultSet rs, int rowNum) throws SQLException {
        return new SubmissionSummaryVO(
            rs.getLong("submission_id"),
            rs.getLong("exam_id"),
            rs.getString("exam_title"),
            rs.getLong("student_id"),
            rs.getString("student_name"),
            rs.getString("submission_status"),
            rs.getBigDecimal("total_score"),
            rs.getBigDecimal("max_score"),
            toLocalDateTime(rs.getTimestamp("submitted_at")),
            toLocalDateTime(rs.getTimestamp("graded_at"))
        );
    }

    private boolean isAutoGradable(String questionType) {
        return "single_choice".equals(questionType)
            || "multiple_choice".equals(questionType)
            || "true_false".equals(questionType)
            || ("fill_blank".equals(questionType) && aesProperties.getGrading().isAutoGradeFillBlank());
    }

    private boolean isAnswerCorrect(AnswerForGrading answer) {
        if ("fill_blank".equals(answer.questionType())) {
            return isFillBlankCorrect(answer.studentAnswer(), answer.correctAnswer(), answer.matchRule());
        }
        return normalizeAnswer(answer.studentAnswer()).equals(normalizeAnswer(answer.correctAnswer()));
    }

    private boolean isFillBlankCorrect(String studentAnswer, String correctAnswer, String matchRule) {
        if (!aesProperties.getGrading().isAutoGradeFillBlank()) {
            return false;
        }
        String rule = matchRule == null ? "exact" : matchRule;
        if ("case_insensitive".equals(rule)) {
            return normalizeFillBlank(studentAnswer).equalsIgnoreCase(normalizeFillBlank(correctAnswer));
        }
        return normalizeFillBlank(studentAnswer).equals(normalizeFillBlank(correctAnswer));
    }

    private String normalizeFillBlank(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("，", ",")
            .replace("、", ",")
            .replaceAll("\\s+", "")
            .trim();
    }

    private String normalizeAnswer(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT)
            .replace("，", ",")
            .replace("、", ",")
            .replaceAll("\\s+", "")
            .trim();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }
        return Boolean.valueOf(value.toString());
    }

    private record AnswerForGrading(
        Long submissionId,
        Long questionId,
        String questionType,
        BigDecimal maxScore,
        String correctAnswer,
        String matchRule,
        String studentAnswer
    ) {
    }
}
