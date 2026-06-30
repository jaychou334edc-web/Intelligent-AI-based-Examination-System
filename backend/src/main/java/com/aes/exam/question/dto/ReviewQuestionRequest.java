package com.aes.exam.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record ReviewQuestionRequest(
    Long parsedQuestionId,

    @NotBlank(message = "题型不能为空")
    String questionType,

    @NotBlank(message = "题干不能为空")
    String stem,

    @Valid
    List<QuestionOptionRequest> options,

    String answer,
    String analysis,

    @NotNull(message = "分值不能为空")
    @DecimalMin(value = "0.1", message = "分值必须大于 0")
    BigDecimal score,

    String knowledgePoint,
    String difficulty,
    String reviewComment
) {
}
