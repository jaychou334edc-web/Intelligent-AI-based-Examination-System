package com.aes.exam.exam.vo;

import java.time.LocalDateTime;

public record SaveAnswerVO(
    Long submissionId,
    Long questionId,
    String status,
    LocalDateTime savedAt
) {
}
