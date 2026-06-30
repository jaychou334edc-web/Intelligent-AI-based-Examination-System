package com.aes.exam.paper.service;

import com.aes.exam.common.config.AesProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final AesProperties properties;

    public FileStorageService(AesProperties properties) {
        this.properties = properties;
    }

    public StoredFile storeImportSource(MultipartFile file) {
        String originalName = file.getOriginalFilename() == null ? "questions.docx" : file.getOriginalFilename();
        String lowerName = originalName.toLowerCase();
        String extension;
        if (lowerName.endsWith(".docx")) {
            extension = ".docx";
        } else if (lowerName.endsWith(".txt")) {
            extension = ".txt";
        } else {
            throw new IllegalArgumentException("仅支持 .docx 或 .txt 文件");
        }

        Path directory = Path.of(properties.getUploadDir(), "papers", LocalDate.now().toString()).toAbsolutePath().normalize();
        String storedName = UUID.randomUUID() + extension;
        Path target = directory.resolve(storedName).normalize();

        try {
            Files.createDirectories(directory);
            file.transferTo(target);
            String hash = sha256(target);
            return new StoredFile(target.toString(), originalName, hash, file.getSize());
        } catch (IOException exception) {
            throw new IllegalStateException("试卷文件保存失败", exception);
        }
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(path);
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                digestInputStream.transferTo(OutputStreamSink.INSTANCE);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    public record StoredFile(String filePath, String originalFileName, String fileHash, long fileSize) {
    }

    private static final class OutputStreamSink extends java.io.OutputStream {
        private static final OutputStreamSink INSTANCE = new OutputStreamSink();

        @Override
        public void write(int value) {
            // discard
        }
    }
}
