package com.aes.exam.question.repository;

import com.aes.exam.question.dto.QuestionOptionRequest;
import com.aes.exam.question.dto.ReviewQuestionRequest;
import com.aes.exam.question.vo.QuestionBankItemVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository {

    Long createQuestion(
        Long sourcePaperId,
        Long sourceAiQuestionId,
        String questionType,
        String stem,
        String analysis,
        BigDecimal score,
        String difficulty,
        String knowledgePoint,
        Long currentUserId
    );

    void createOptions(Long questionId, List<QuestionOptionRequest> options, String answer);

    void createAnswer(Long questionId, String answer);

    List<QuestionBankItemVO> findRecent(int limit);

    Optional<QuestionBankItemVO> findById(Long questionId);

    boolean existsActive(Long questionId);

    boolean isUsedInExam(Long questionId);

    void updateQuestion(Long questionId, ReviewQuestionRequest request, Long currentUserId);

    void softDelete(Long questionId, Long currentUserId);
}
