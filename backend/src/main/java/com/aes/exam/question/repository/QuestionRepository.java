package com.aes.exam.question.repository;

import com.aes.exam.question.dto.QuestionOptionRequest;
import com.aes.exam.question.vo.QuestionBankItemVO;
import java.math.BigDecimal;
import java.util.List;

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
}
