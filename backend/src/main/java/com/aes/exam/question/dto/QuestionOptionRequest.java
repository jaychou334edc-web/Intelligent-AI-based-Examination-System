package com.aes.exam.question.dto;

import jakarta.validation.constraints.NotBlank;

public record QuestionOptionRequest(
    @NotBlank(message = "选项标识不能为空")
    String key,

    @NotBlank(message = "选项内容不能为空")
    String text
) {
}
