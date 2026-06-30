package com.aes.exam.question.vo;

import java.math.BigDecimal;
import java.util.List;

public record ReviewQuestionVO(
    Long parsedQuestionId,
    String questionType,
    String stem,
    List<QuestionOptionVO> options,
    String answer,
    String analysis,
    BigDecimal score,
    String knowledgePoint,
    String difficulty,
    String reviewStatus,
    String reviewComment
) {
}
