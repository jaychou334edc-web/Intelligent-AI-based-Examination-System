package com.aes.exam.monitoring.repository;

import com.aes.exam.monitoring.vo.AntiCheatEventVO;
import com.aes.exam.monitoring.vo.DifficultyStatsVO;
import com.aes.exam.monitoring.vo.EventCountVO;
import com.aes.exam.monitoring.vo.EventRecordedVO;
import com.aes.exam.monitoring.vo.ExamAnalyticsVO;
import com.aes.exam.monitoring.vo.KnowledgePointStatsVO;
import com.aes.exam.monitoring.vo.QuestionAccuracyVO;
import com.aes.exam.monitoring.vo.ScoreBucketVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMonitoringRepository implements MonitoringRepository {

    private static final TypeReference<Map<String, Object>> EVENT_DATA_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcMonitoringRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isStudentAssignedToExam(Long examId, Long studentId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM exam_participants ep
            JOIN exams e ON e.id = ep.exam_id
            WHERE ep.exam_id = ? AND ep.student_id = ? AND e.status = 'published' AND e.is_deleted = 0
            """, Integer.class, examId, studentId);
        return count != null && count > 0;
    }

    @Override
    public boolean isTeacherExamOwner(Long examId, Long teacherId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM exams
            WHERE id = ? AND created_by = ? AND is_deleted = 0
            """, Integer.class, examId, teacherId);
        return count != null && count > 0;
    }

    @Override
    public EventRecordedVO recordEvent(
        Long examId,
        Long studentId,
        String eventType,
        String eventLevel,
        String eventDataJson,
        LocalDateTime clientTime
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO anti_cheat_events (user_id, exam_id, event_type, event_level, event_data, client_time, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, new String[]{"id"});
            statement.setLong(1, studentId);
            statement.setLong(2, examId);
            statement.setString(3, eventType);
            statement.setString(4, eventLevel);
            statement.setString(5, eventDataJson);
            statement.setObject(6, clientTime);
            return statement;
        }, keyHolder);
        return new EventRecordedVO(keyHolder.getKey().longValue(), "recorded", LocalDateTime.now());
    }

    @Override
    public List<AntiCheatEventVO> findEventsForTeacher(Long examId, Long teacherId) {
        return jdbcTemplate.query("""
            SELECT ace.id, ace.user_id, COALESCE(up.real_name, u.username) AS student_name,
                   ace.exam_id, e.title AS exam_title, ace.event_type, ace.event_level, ace.event_data,
                   ace.client_time, ace.created_at
            FROM anti_cheat_events ace
            JOIN exams e ON e.id = ace.exam_id
            JOIN users u ON u.id = ace.user_id
            LEFT JOIN user_profiles up ON up.user_id = u.id
            WHERE ace.exam_id = ? AND e.created_by = ? AND e.is_deleted = 0
            ORDER BY ace.created_at DESC, ace.id DESC
            """, this::mapEvent, examId, teacherId);
    }

    @Override
    public Optional<ExamAnalyticsVO> findAnalyticsForTeacher(Long examId, Long teacherId) {
        List<AnalyticsBase> bases = jdbcTemplate.query("""
            SELECT e.id AS exam_id, e.title AS exam_title,
                   COUNT(DISTINCT ep.student_id) AS participant_count,
                   COUNT(DISTINCT CASE WHEN s.status = 'submitted' THEN s.id END) AS submitted_count,
                   AVG(CASE WHEN s.status = 'submitted' THEN s.total_score END) AS average_score,
                   MAX(CASE WHEN s.status = 'submitted' THEN s.total_score END) AS max_score,
                   MIN(CASE WHEN s.status = 'submitted' THEN s.total_score END) AS min_score
            FROM exams e
            LEFT JOIN exam_participants ep ON ep.exam_id = e.id
            LEFT JOIN submissions s ON s.exam_id = e.id AND s.student_id = ep.student_id
            WHERE e.id = ? AND e.created_by = ? AND e.is_deleted = 0
            GROUP BY e.id, e.title
            """, this::mapAnalyticsBase, examId, teacherId);
        if (bases.isEmpty()) {
            return Optional.empty();
        }

        AnalyticsBase base = bases.get(0);
        return Optional.of(new ExamAnalyticsVO(
            base.examId(),
            base.examTitle(),
            base.participantCount(),
            base.submittedCount(),
            scale(base.averageScore()),
            scale(base.maxScore()),
            scale(base.minScore()),
            passRate(examId, examMaxScore(examId)),
            findScoreDistribution(examId),
            findQuestionAccuracy(examId),
            findDifficultyStats(examId),
            findKnowledgePointStats(examId),
            findEventCounts(examId)
        ));
    }

    @Override
    public List<ScoreBucketVO> findScoreDistribution(Long examId) {
        BigDecimal maxScore = examMaxScore(examId);
        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("0-59", 0L);
        buckets.put("60-69", 0L);
        buckets.put("70-79", 0L);
        buckets.put("80-89", 0L);
        buckets.put("90-100", 0L);
        if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return buckets.entrySet().stream().map(entry -> new ScoreBucketVO(entry.getKey(), entry.getValue())).toList();
        }

        List<BigDecimal> scores = jdbcTemplate.query("""
            SELECT total_score
            FROM submissions
            WHERE exam_id = ? AND status = 'submitted'
            """, (rs, rowNum) -> rs.getBigDecimal("total_score"), examId);
        for (BigDecimal score : scores) {
            BigDecimal percent = percentage(score, maxScore);
            String label = scoreBucket(percent);
            buckets.put(label, buckets.get(label) + 1);
        }
        return buckets.entrySet().stream().map(entry -> new ScoreBucketVO(entry.getKey(), entry.getValue())).toList();
    }

    @Override
    public List<QuestionAccuracyVO> findQuestionAccuracy(Long examId) {
        return jdbcTemplate.query("""
            SELECT q.id AS question_id, q.question_type, q.stem, eq.score AS max_score,
                   AVG(gr.final_score) AS average_score
            FROM exam_questions eq
            JOIN questions q ON q.id = eq.question_id
            LEFT JOIN submissions s ON s.exam_id = eq.exam_id AND s.status = 'submitted'
            LEFT JOIN grading_records gr ON gr.submission_id = s.id AND gr.question_id = q.id
            WHERE eq.exam_id = ?
            GROUP BY q.id, q.question_type, q.stem, eq.score, eq.sort_order
            ORDER BY eq.sort_order ASC, q.id ASC
            """, (rs, rowNum) -> {
                BigDecimal maxScore = rs.getBigDecimal("max_score");
                BigDecimal averageScore = scale(rs.getBigDecimal("average_score"));
                return new QuestionAccuracyVO(
                    rs.getLong("question_id"),
                    rs.getString("question_type"),
                    rs.getString("stem"),
                    maxScore,
                    averageScore,
                    percentage(averageScore, maxScore)
                );
            }, examId);
    }

    @Override
    public List<DifficultyStatsVO> findDifficultyStats(Long examId) {
        return jdbcTemplate.query("""
            SELECT COALESCE(q.difficulty, '未标注') AS difficulty, COUNT(DISTINCT q.id) AS question_count,
                   AVG(gr.final_score) AS average_score, AVG(eq.score) AS average_max_score
            FROM exam_questions eq
            JOIN questions q ON q.id = eq.question_id
            LEFT JOIN submissions s ON s.exam_id = eq.exam_id AND s.status = 'submitted'
            LEFT JOIN grading_records gr ON gr.submission_id = s.id AND gr.question_id = q.id
            WHERE eq.exam_id = ?
            GROUP BY COALESCE(q.difficulty, '未标注')
            ORDER BY difficulty ASC
            """, (rs, rowNum) -> {
                BigDecimal averageScore = scale(rs.getBigDecimal("average_score"));
                BigDecimal averageMaxScore = rs.getBigDecimal("average_max_score");
                return new DifficultyStatsVO(
                    rs.getString("difficulty"),
                    rs.getLong("question_count"),
                    averageScore,
                    percentage(averageScore, averageMaxScore)
                );
            }, examId);
    }

    @Override
    public List<KnowledgePointStatsVO> findKnowledgePointStats(Long examId) {
        return jdbcTemplate.query("""
            SELECT COALESCE(NULLIF(q.knowledge_point, ''), '未标注') AS knowledge_point,
                   COUNT(DISTINCT q.id) AS question_count, AVG(gr.final_score) AS average_score,
                   AVG(eq.score) AS average_max_score
            FROM exam_questions eq
            JOIN questions q ON q.id = eq.question_id
            LEFT JOIN submissions s ON s.exam_id = eq.exam_id AND s.status = 'submitted'
            LEFT JOIN grading_records gr ON gr.submission_id = s.id AND gr.question_id = q.id
            WHERE eq.exam_id = ?
            GROUP BY COALESCE(NULLIF(q.knowledge_point, ''), '未标注')
            ORDER BY knowledge_point ASC
            """, (rs, rowNum) -> {
                BigDecimal averageScore = scale(rs.getBigDecimal("average_score"));
                BigDecimal averageMaxScore = rs.getBigDecimal("average_max_score");
                return new KnowledgePointStatsVO(
                    rs.getString("knowledge_point"),
                    rs.getLong("question_count"),
                    averageScore,
                    percentage(averageScore, averageMaxScore)
                );
            }, examId);
    }

    @Override
    public List<EventCountVO> findEventCounts(Long examId) {
        return jdbcTemplate.query("""
            SELECT event_type, COUNT(*) AS event_count
            FROM anti_cheat_events
            WHERE exam_id = ?
            GROUP BY event_type
            ORDER BY event_count DESC, event_type ASC
            """, (rs, rowNum) -> new EventCountVO(rs.getString("event_type"), rs.getLong("event_count")), examId);
    }

    @Override
    public Map<String, Object> emptyEventData() {
        return Map.of();
    }

    private AntiCheatEventVO mapEvent(ResultSet rs, int rowNum) throws SQLException {
        return new AntiCheatEventVO(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("student_name"),
            rs.getLong("exam_id"),
            rs.getString("exam_title"),
            rs.getString("event_type"),
            rs.getString("event_level"),
            parseEventData(rs.getString("event_data")),
            toLocalDateTime(rs.getTimestamp("client_time")),
            toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private AnalyticsBase mapAnalyticsBase(ResultSet rs, int rowNum) throws SQLException {
        return new AnalyticsBase(
            rs.getLong("exam_id"),
            rs.getString("exam_title"),
            rs.getInt("participant_count"),
            rs.getInt("submitted_count"),
            rs.getBigDecimal("average_score"),
            rs.getBigDecimal("max_score"),
            rs.getBigDecimal("min_score")
        );
    }

    private BigDecimal passRate(Long examId, BigDecimal maxScore) {
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> scores = jdbcTemplate.query("""
            SELECT total_score
            FROM submissions
            WHERE exam_id = ? AND status = 'submitted'
            """, (rs, rowNum) -> rs.getBigDecimal("total_score"), examId);
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal passLine = maxScore.multiply(BigDecimal.valueOf(0.6));
        long passed = scores.stream()
            .filter(score -> score != null && score.compareTo(passLine) >= 0)
            .count();
        return BigDecimal.valueOf(passed)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal examMaxScore(Long examId) {
        BigDecimal maxScore = jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(score), 0)
            FROM exam_questions
            WHERE exam_id = ?
            """, BigDecimal.class, examId);
        return maxScore == null ? BigDecimal.ZERO : maxScore;
    }

    private String scoreBucket(BigDecimal percent) {
        if (percent.compareTo(BigDecimal.valueOf(60)) < 0) {
            return "0-59";
        }
        if (percent.compareTo(BigDecimal.valueOf(70)) < 0) {
            return "60-69";
        }
        if (percent.compareTo(BigDecimal.valueOf(80)) < 0) {
            return "70-79";
        }
        if (percent.compareTo(BigDecimal.valueOf(90)) < 0) {
            return "80-89";
        }
        return "90-100";
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal maxValue) {
        if (value == null || maxValue == null || maxValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(BigDecimal.valueOf(100)).divide(maxValue, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> parseEventData(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, EVENT_DATA_TYPE);
        } catch (Exception exception) {
            return Map.of("raw", json);
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record AnalyticsBase(
        Long examId,
        String examTitle,
        Integer participantCount,
        Integer submittedCount,
        BigDecimal averageScore,
        BigDecimal maxScore,
        BigDecimal minScore
    ) {
    }
}
