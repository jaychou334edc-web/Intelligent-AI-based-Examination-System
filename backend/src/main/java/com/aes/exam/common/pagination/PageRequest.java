package com.aes.exam.common.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageRequest(
    @Min(value = 1, message = "页码必须大于等于 1")
    Integer page,

    @Min(value = 1, message = "每页数量必须大于等于 1")
    @Max(value = 200, message = "每页数量不能超过 200")
    Integer size
) {

    public PageRequest {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
    }

    public int offset() {
        return (page - 1) * size;
    }
}
