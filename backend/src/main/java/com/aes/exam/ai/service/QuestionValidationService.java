package com.aes.exam.ai.service;

import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QuestionValidationService {

    private static final Set<String> TYPES = Set.of("single_choice", "multiple_choice", "true_false", "fill_blank", "subjective");

    public void validate(ParsedQuestionModel question) {
        if (!StringUtils.hasText(question.getQuestionType()) || !TYPES.contains(question.getQuestionType())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "AI 返回了无法识别的题型");
        }
        if (!StringUtils.hasText(question.getStem())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "AI 返回的题干为空");
        }
        if (question.getScore() == null || question.getScore().compareTo(BigDecimal.ZERO) <= 0) {
            question.setScore(BigDecimal.valueOf(5));
        }
        if ((question.getQuestionType().equals("single_choice") || question.getQuestionType().equals("multiple_choice"))
            && question.getOptions().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "选择题缺少选项");
        }
    }
}
