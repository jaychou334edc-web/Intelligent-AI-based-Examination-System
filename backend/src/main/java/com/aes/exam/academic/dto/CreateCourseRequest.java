package com.aes.exam.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
    @NotBlank @Size(max = 128) String name,
    @Size(max = 64) String code,
    @Size(max = 2000) String description,
    Long teacherId
) {
}
