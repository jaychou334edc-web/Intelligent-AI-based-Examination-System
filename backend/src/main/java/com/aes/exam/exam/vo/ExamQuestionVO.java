package com.aes.exam.exam.vo;

import com.aes.exam.question.vo.QuestionOptionVO;
import java.math.BigDecimal;
import java.util.List;

public record ExamQuestionVO(
    Long id,
    Long sourcePaperId,
    String questionType,
    String stem,
    String analysis,
    BigDecimal score,
    String answer,
    List<QuestionOptionVO> options,
    Integer sortOrder,
    String savedAnswer
) {
}
