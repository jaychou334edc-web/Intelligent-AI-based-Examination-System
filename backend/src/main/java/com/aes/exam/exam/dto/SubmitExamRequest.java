package com.aes.exam.exam.dto;

import java.util.List;

public record SubmitExamRequest(
    List<SaveAnswerRequest> answers
) {
}
