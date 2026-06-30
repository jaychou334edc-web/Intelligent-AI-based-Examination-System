package com.aes.exam.exam.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdateExamQuestionsRequest(
    @NotEmpty List<Long> questionIds
) {
}
