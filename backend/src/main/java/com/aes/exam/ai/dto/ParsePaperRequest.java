package com.aes.exam.ai.dto;

import jakarta.validation.constraints.NotNull;

public record ParsePaperRequest(
    @NotNull(message = "试卷 ID 不能为空")
    Long paperId
) {
}
