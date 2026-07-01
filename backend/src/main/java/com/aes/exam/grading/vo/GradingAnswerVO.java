package com.aes.exam.grading.vo;

import com.aes.exam.question.vo.QuestionOptionVO;
import java.math.BigDecimal;
import java.util.List;

public record GradingAnswerVO(
    Long questionId,
    Long sourcePaperId,
    String questionType,
    String stem,
    List<QuestionOptionVO> options,
    BigDecimal maxScore,
    String correctAnswer,
    String studentAnswer,
    Boolean isCorrect,
    BigDecimal autoScore,
    BigDecimal aiSuggestionScore,
    String aiComment,
    BigDecimal manualScore,
    BigDecimal finalScore,
    String gradingStatus,
    String teacherComment
) {
}
