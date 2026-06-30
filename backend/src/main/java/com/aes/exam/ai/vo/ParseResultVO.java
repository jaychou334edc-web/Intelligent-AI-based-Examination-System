package com.aes.exam.ai.vo;

import com.aes.exam.question.vo.ReviewQuestionVO;
import java.util.List;

public record ParseResultVO(
    Long paperId,
    Long parseJobId,
    String status,
    String rawText,
    List<ReviewQuestionVO> questions
) {
}
