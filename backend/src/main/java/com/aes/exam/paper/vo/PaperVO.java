package com.aes.exam.paper.vo;

import java.time.LocalDateTime;

public record PaperVO(
    Long id,
    String title,
    String fileName,
    long fileSize,
    String parseStatus,
    LocalDateTime uploadTime
) {
}
