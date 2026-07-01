package com.aes.exam.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateTeachingClassRequest(
    @NotNull Long courseId,
    @NotBlank @Size(max = 128) String name,
    @Size(max = 64) String grade,
    @Size(max = 128) String major,
    List<Long> studentIds
) {
}
