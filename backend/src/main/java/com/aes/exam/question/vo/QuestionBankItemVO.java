package com.aes.exam.question.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record QuestionBankItemVO(
    Long id,
    Long sourcePaperId,
    String questionType,
    String stem,
    String analysis,
    BigDecimal score,
    String difficulty,
    String knowledgePoint,
    String answer,
    List<QuestionOptionVO> options,
    LocalDateTime createdAt
) {
}
