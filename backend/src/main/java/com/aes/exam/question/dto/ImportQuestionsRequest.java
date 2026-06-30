package com.aes.exam.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ImportQuestionsRequest(
    @NotNull(message = "试卷 ID 不能为空")
    Long paperId,

    @Valid
    @NotEmpty(message = "导入题目不能为空")
    List<ReviewQuestionRequest> questions
) {
}
