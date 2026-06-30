package com.aes.exam.paper.entity;

import java.time.LocalDateTime;

public record PaperEntity(
    Long id,
    String title,
    String filePath,
    String fileName,
    String fileHash,
    long fileSize,
    Long uploadUserId,
    LocalDateTime uploadTime,
    String parseStatus,
    String aiModel,
    String rawText,
    String imageManifestJson
) {
}
