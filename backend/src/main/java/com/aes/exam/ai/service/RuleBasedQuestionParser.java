package com.aes.exam.ai.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class RuleBasedQuestionParser {

    private static final Pattern QUESTION_START = Pattern.compile("^\\s*(\\d+)[.、．]\\s*(.+)$");
    private static final Pattern CHINESE_QUESTION_START = Pattern.compile("^\\s*题目\\s*(\\d+)\\s*[、:：]\\s*(.+)$");
    private static final Pattern SUB_QUESTION_START = Pattern.compile("^\\s*【问题\\s*(\\d+)】\\s*(.*)$");
    private static final Pattern OPTION = Pattern.compile("^\\s*([A-Ha-h])[.、．]\\s*(.+)$");
    private static final Pattern ANSWER = Pattern.compile("^\\s*(答案|参考答案|正确答案)[:：]?\\s*(.+)$");
    private static final Pattern SCORE = Pattern.compile("[（(]?\\s*(\\d+(?:\\.\\d+)?)\\s*分\\s*[）)]?");

    public List<String> segment(String rawText) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : normalizedLines(rawText)) {
            if (isQuestionBoundary(line) && !current.isEmpty()) {
                blocks.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(line).append('\n');
        }
        if (!current.isEmpty()) {
            blocks.add(current.toString().trim());
        }

        if (blocks.isEmpty() && rawText != null && !rawText.isBlank()) {
            blocks.add(rawText.trim());
        }
        return blocks;
    }

    public StructuredParseResponse parse(List<String> blocks) {
        StructuredParseResponse response = new StructuredParseResponse();
        List<ParsedQuestionModel> questions = blocks.stream()
            .map(this::parseBlock)
            .filter(question -> question.getStem() != null && !question.getStem().isBlank())
            .toList();
        response.setQuestions(questions);
        return response;
    }

    private ParsedQuestionModel parseBlock(String block) {
        ParsedQuestionModel question = new ParsedQuestionModel();
        List<String> stemLines = new ArrayList<>();
        List<ParsedQuestionOptionModel> options = new ArrayList<>();

        for (String line : normalizedLines(block)) {
            Matcher answerMatcher = ANSWER.matcher(line);
            Matcher optionMatcher = OPTION.matcher(line);

            if (answerMatcher.matches()) {
                question.setAnswer(answerMatcher.group(2).trim());
            } else if (optionMatcher.matches()) {
                options.add(new ParsedQuestionOptionModel(
                    optionMatcher.group(1).toUpperCase(Locale.ROOT),
                    optionMatcher.group(2).trim()
                ));
            } else {
                stemLines.add(stripQuestionNumber(line));
            }
        }

        String stem = String.join("\n", stemLines).trim();
        question.setScore(extractScore(stem));
        question.setStem(SCORE.matcher(stem).replaceAll("").trim());
        question.setOptions(options);
        question.setQuestionType(detectQuestionType(question));
        question.setDifficulty("normal");
        question.setKnowledgePoint("");
        return question;
    }

    private List<String> normalizedLines(String text) {
        if (text == null) {
            return List.of();
        }
        return text.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .toList();
    }

    private String stripQuestionNumber(String line) {
        Matcher matcher = QUESTION_START.matcher(line);
        if (matcher.matches()) {
            return matcher.group(2).trim();
        }
        Matcher chineseMatcher = CHINESE_QUESTION_START.matcher(line);
        if (chineseMatcher.matches()) {
            return chineseMatcher.group(2).trim();
        }
        Matcher subQuestionMatcher = SUB_QUESTION_START.matcher(line);
        if (subQuestionMatcher.matches()) {
            String suffix = subQuestionMatcher.group(2).trim();
            return suffix.isBlank() ? "问题" + subQuestionMatcher.group(1) : suffix;
        }
        return line;
    }

    private boolean isQuestionBoundary(String line) {
        return QUESTION_START.matcher(line).matches()
            || CHINESE_QUESTION_START.matcher(line).matches()
            || SUB_QUESTION_START.matcher(line).matches();
    }

    private BigDecimal extractScore(String stem) {
        Matcher matcher = SCORE.matcher(stem);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1));
        }
        return BigDecimal.valueOf(5);
    }

    private String detectQuestionType(ParsedQuestionModel question) {
        String stem = question.getStem() == null ? "" : question.getStem();
        String answer = question.getAnswer() == null ? "" : question.getAnswer();
        if (!question.getOptions().isEmpty()) {
            return answer.matches(".*[A-Ha-h].*[,，、\\s]+.*[A-Ha-h].*") ? "multiple_choice" : "single_choice";
        }
        if (stem.contains("判断") || answer.equals("对") || answer.equals("错") || answer.equalsIgnoreCase("true") || answer.equalsIgnoreCase("false")) {
            return "true_false";
        }
        if (stem.contains("____") || stem.contains("填空")) {
            return "fill_blank";
        }
        return "subjective";
    }
}
