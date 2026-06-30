package com.aes.exam.ai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class QuestionChunker {

    private static final int MAX_CHARS = 2500;
    private static final Pattern STRONG_BOUNDARY = Pattern.compile(
        "^(题目\\s*\\d+\\s*[、:：].*|\\d+\\s*[.、．]\\s*\\S+.*|[一二三四五六七八九十]+、\\S+.*)$"
    );
    private static final Pattern WEAK_BOUNDARY = Pattern.compile("^【问题\\s*\\d+】.*$");

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> paragraphs = text.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .toList();
        List<String> coarseBlocks = coarseBlocks(paragraphs);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String block : coarseBlocks) {
            if (!current.isEmpty() && current.length() + block.length() + 1 > MAX_CHARS) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            if (block.length() > MAX_CHARS) {
                chunks.addAll(splitLongBlock(block));
            } else {
                current.append(block).append('\n');
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks.isEmpty() ? List.of(text.trim()) : chunks;
    }

    private List<String> coarseBlocks(List<String> paragraphs) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (STRONG_BOUNDARY.matcher(paragraph).matches() && !current.isEmpty()) {
                blocks.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(paragraph).append('\n');
        }
        if (!current.isEmpty()) {
            blocks.add(current.toString().trim());
        }
        return blocks;
    }

    private List<String> splitLongBlock(String block) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : block.lines().toList()) {
            boolean boundary = WEAK_BOUNDARY.matcher(line.trim()).matches();
            if (boundary && current.length() > MAX_CHARS / 2) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            } else if (!current.isEmpty() && current.length() + line.length() + 1 > MAX_CHARS) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(line).append('\n');
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }
}
