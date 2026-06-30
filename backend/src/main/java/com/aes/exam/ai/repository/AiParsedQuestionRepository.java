package com.aes.exam.ai.repository;

import com.aes.exam.ai.entity.AiParsedQuestionEntity;
import java.math.BigDecimal;
import java.util.List;

public interface AiParsedQuestionRepository {

    void deleteByPaperId(Long paperId);

    Long create(Long paperId, Long parseJobId, String questionJson, BigDecimal confidenceScore, String reviewStatus);

    List<AiParsedQuestionEntity> findByPaperId(Long paperId);

    void markReviewed(Long id, String reviewStatus, String reviewComment, String questionJson);
}
