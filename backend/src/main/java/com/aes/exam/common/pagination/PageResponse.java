package com.aes.exam.common.pagination;

import java.util.List;

public record PageResponse<T>(
    List<T> items,
    int page,
    int size,
    long total,
    long totalPages
) {

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long total) {
        long totalPages = size <= 0 ? 0 : (long) Math.ceil((double) total / size);
        return new PageResponse<>(items, page, size, total, totalPages);
    }
}
