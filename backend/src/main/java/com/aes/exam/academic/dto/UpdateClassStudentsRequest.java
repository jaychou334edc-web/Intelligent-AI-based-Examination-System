package com.aes.exam.academic.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateClassStudentsRequest(
    @NotNull List<Long> studentIds
) {
}
