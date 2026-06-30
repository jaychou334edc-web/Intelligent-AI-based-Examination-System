package com.aes.exam.monitoring.repository;

import com.aes.exam.monitoring.vo.AntiCheatEventVO;
import com.aes.exam.monitoring.vo.DifficultyStatsVO;
import com.aes.exam.monitoring.vo.EventCountVO;
import com.aes.exam.monitoring.vo.EventRecordedVO;
import com.aes.exam.monitoring.vo.ExamAnalyticsVO;
import com.aes.exam.monitoring.vo.KnowledgePointStatsVO;
import com.aes.exam.monitoring.vo.QuestionAccuracyVO;
import com.aes.exam.monitoring.vo.ScoreBucketVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MonitoringRepository {

    boolean isStudentAssignedToExam(Long examId, Long studentId);

    boolean isTeacherExamOwner(Long examId, Long teacherId);

    EventRecordedVO recordEvent(
        Long examId,
        Long studentId,
        String eventType,
        String eventLevel,
        String eventDataJson,
        LocalDateTime clientTime
    );

    List<AntiCheatEventVO> findEventsForTeacher(Long examId, Long teacherId);

    Optional<ExamAnalyticsVO> findAnalyticsForTeacher(Long examId, Long teacherId);

    List<ScoreBucketVO> findScoreDistribution(Long examId);

    List<QuestionAccuracyVO> findQuestionAccuracy(Long examId);

    List<DifficultyStatsVO> findDifficultyStats(Long examId);

    List<KnowledgePointStatsVO> findKnowledgePointStats(Long examId);

    List<EventCountVO> findEventCounts(Long examId);

    Map<String, Object> emptyEventData();
}
