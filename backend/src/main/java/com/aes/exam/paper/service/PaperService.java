package com.aes.exam.paper.service;

import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.BusinessException;
import com.aes.exam.common.security.SecurityContext;
import com.aes.exam.common.security.SecurityContextHolder;
import com.aes.exam.paper.entity.PaperEntity;
import com.aes.exam.paper.repository.PaperRepository;
import com.aes.exam.paper.service.FileStorageService.StoredFile;
import com.aes.exam.paper.vo.PaperVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PaperService {

    private final FileStorageService fileStorageService;
    private final PaperRepository paperRepository;

    public PaperService(FileStorageService fileStorageService, PaperRepository paperRepository) {
        this.fileStorageService = fileStorageService;
        this.paperRepository = paperRepository;
    }

    @Transactional
    public PaperVO upload(MultipartFile file, String title) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "请上传题库源文件");
        }

        SecurityContext context = SecurityContextHolder.current();
        if (context == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        StoredFile storedFile = fileStorageService.storeImportSource(file);
        Long paperId = paperRepository.create(
            title,
            storedFile.filePath(),
            storedFile.originalFileName(),
            storedFile.fileHash(),
            storedFile.fileSize(),
            context.userId(),
            LocalDateTime.now()
        );
        return toVO(getRequired(paperId));
    }

    public PaperEntity getRequired(Long paperId) {
        return paperRepository.findById(paperId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "试卷不存在"));
    }

    public PaperVO toVO(PaperEntity entity) {
        return new PaperVO(
            entity.id(),
            entity.title(),
            entity.fileName(),
            entity.fileSize(),
            entity.parseStatus(),
            entity.uploadTime()
        );
    }
}
