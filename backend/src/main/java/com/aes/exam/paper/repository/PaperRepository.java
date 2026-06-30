package com.aes.exam.paper.repository;

import com.aes.exam.paper.entity.PaperEntity;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PaperRepository {

    Long create(String title, String filePath, String fileName, String fileHash, long fileSize, Long uploadUserId, LocalDateTime uploadTime);

    Optional<PaperEntity> findById(Long id);

    void updateRawTextAndStatus(Long id, String rawText, String parseStatus, String aiModel);
}
