package com.aes.exam.paper.service;

import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.BusinessException;
import com.aes.exam.paper.entity.PaperEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PaperImageService {

    private final PaperService paperService;
    private final ObjectMapper objectMapper;

    public PaperImageService(PaperService paperService, ObjectMapper objectMapper) {
        this.paperService = paperService;
        this.objectMapper = objectMapper;
    }

    public PaperImageResource getImage(Long paperId, String imageId) {
        PaperEntity paper = paperService.getRequired(paperId);
        List<ImageManifestItem> images = readManifest(paper.imageManifestJson());
        ImageManifestItem image = images.stream()
            .filter(item -> item.id().equals(imageId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "图片不存在"));

        Path imagePath = Path.of(image.filePath()).toAbsolutePath().normalize();
        Path paperPath = Path.of(paper.filePath()).toAbsolutePath().normalize();
        if (!imagePath.startsWith(paperPath.getParent())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "图片路径非法");
        }

        FileSystemResource resource = new FileSystemResource(imagePath);
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片文件不存在");
        }
        return new PaperImageResource(resource, toMediaType(image.contentType()));
    }

    public String toManifestJson(List<DocxExtractionService.ImageReference> images) {
        try {
            List<ImageManifestItem> items = images.stream()
                .map(image -> new ImageManifestItem(
                    image.id(),
                    image.fileName(),
                    image.contentType(),
                    image.size(),
                    image.filePath()
                ))
                .toList();
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("图片清单序列化失败", exception);
        }
    }

    private List<ImageManifestItem> readManifest(String manifestJson) {
        if (!StringUtils.hasText(manifestJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(manifestJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("图片清单解析失败", exception);
        }
    }

    private MediaType toMediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }

    public record PaperImageResource(Resource resource, MediaType mediaType) {
    }

    public record ImageManifestItem(String id, String fileName, String contentType, int size, String filePath) {
    }
}
