package com.aes.exam.paper.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

@Service
public class DocxExtractionService {

    public String extractText(Path filePath) {
        return extract(filePath).text();
    }

    public ExtractedDocument extract(Path filePath) {
        String lowerName = filePath.getFileName().toString().toLowerCase();
        if (lowerName.endsWith(".txt")) {
            return extractTxt(filePath);
        }
        if (!lowerName.endsWith(".docx")) {
            throw new IllegalArgumentException("仅支持 .docx 或 .txt 文件");
        }

        try (InputStream inputStream = Files.newInputStream(filePath);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            ImageRegistry imageRegistry = new ImageRegistry(document.getAllPictures());
            StringBuilder text = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                if (element.getElementType() == BodyElementType.PARAGRAPH) {
                    appendParagraph(text, (XWPFParagraph) element, imageRegistry);
                } else if (element.getElementType() == BodyElementType.TABLE) {
                    appendTable(text, (XWPFTable) element, imageRegistry);
                }
            }
            return new ExtractedDocument(text.toString().trim(), imageRegistry.images());
        } catch (IOException exception) {
            throw new IllegalStateException("Word 文档提取失败", exception);
        }
    }

    private ExtractedDocument extractTxt(Path filePath) {
        try {
            return new ExtractedDocument(Files.readString(filePath, StandardCharsets.UTF_8).trim(), List.of());
        } catch (IOException exception) {
            throw new IllegalStateException("TXT 文件提取失败", exception);
        }
    }

    private void appendParagraph(StringBuilder text, XWPFParagraph paragraph, ImageRegistry imageRegistry) {
        StringBuilder line = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String value = run.text();
            if (value != null && !value.isBlank()) {
                line.append(value);
            }
            for (XWPFPicture picture : run.getEmbeddedPictures()) {
                line.append(" ").append(imageRegistry.placeholderFor(picture.getPictureData())).append(" ");
            }
        }
        String normalized = line.toString().trim();
        if (!normalized.isBlank()) {
            text.append(normalized).append('\n');
        }
    }

    private void appendTable(StringBuilder text, XWPFTable table, ImageRegistry imageRegistry) {
        text.append("[表格开始]\n");
        for (XWPFTableRow row : table.getRows()) {
            StringJoiner cells = new StringJoiner(" | ");
            for (XWPFTableCell cell : row.getTableCells()) {
                StringBuilder cellText = new StringBuilder();
                for (IBodyElement cellElement : cell.getBodyElements()) {
                    if (cellElement.getElementType() == BodyElementType.PARAGRAPH) {
                        appendParagraph(cellText, (XWPFParagraph) cellElement, imageRegistry);
                    } else if (cellElement.getElementType() == BodyElementType.TABLE) {
                        appendTable(cellText, (XWPFTable) cellElement, imageRegistry);
                    }
                }
                cells.add(cellText.toString().replace('\n', ' ').trim());
            }
            text.append(cells).append('\n');
        }
        text.append("[表格结束]\n");
    }

    private static final class ImageRegistry {
        private final Map<String, String> relationIds = new HashMap<>();
        private final List<ImageReference> images = new ArrayList<>();

        private ImageRegistry(List<XWPFPictureData> pictures) {
            for (XWPFPictureData picture : pictures) {
                String id = "image_" + (images.size() + 1);
                relationIds.put(packageName(picture), id);
                images.add(new ImageReference(
                    id,
                    picture.getFileName(),
                    picture.getPackagePart().getContentType(),
                    picture.getData().length
                ));
            }
        }

        private String placeholderFor(XWPFPictureData picture) {
            String id = relationIds.getOrDefault(packageName(picture), "image_" + (images.size() + 1));
            return "[IMG:" + id + "]";
        }

        private String packageName(XWPFPictureData picture) {
            return picture.getPackagePart().getPartName().getName();
        }

        private List<ImageReference> images() {
            return List.copyOf(images);
        }
    }

    public record ExtractedDocument(String text, List<ImageReference> images) {
    }

    public record ImageReference(String id, String fileName, String contentType, int size) {
    }
}
