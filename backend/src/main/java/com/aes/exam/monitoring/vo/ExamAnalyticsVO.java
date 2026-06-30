package com.aes.exam.monitoring.vo;

import java.math.BigDecimal;
import java.util.List;

public record ExamAnalyticsVO(
    Long examId,
    String examTitle,
    Integer participantCount,
    Integer submittedCount,
    BigDecimal averageScore,
    BigDecimal maxScore,
    BigDecimal minScore,
    BigDecimal passRate,
    List<ScoreBucketVO> scoreDistribution,
    List<QuestionAccuracyVO> questionAccuracy,
    List<DifficultyStatsVO> difficultyStats,
    List<KnowledgePointStatsVO> knowledgePointStats,
    List<EventCountVO> eventCounts
) {
}
