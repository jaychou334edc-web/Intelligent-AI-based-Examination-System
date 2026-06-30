package com.aes.exam.ai.service;

import com.aes.exam.question.vo.QuestionOptionVO;
import com.aes.exam.question.vo.ReviewQuestionVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class QuestionJsonMapper {

    private final ObjectMapper objectMapper;

    public QuestionJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(ParsedQuestionModel question) {
        try {
            return objectMapper.writeValueAsString(question);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("题目 JSON 序列化失败", exception);
        }
    }

    public String toJson(ReviewQuestionVO question) {
        try {
            return objectMapper.writeValueAsString(question);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("题目 JSON 序列化失败", exception);
        }
    }

    public String toJson(StructuredParseResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("解析结果 JSON 序列化失败", exception);
        }
    }

    public ReviewQuestionVO toReviewQuestion(Long parsedQuestionId, String questionJson, String reviewStatus, String reviewComment) {
        try {
            ParsedQuestionModel question = objectMapper.readValue(questionJson, ParsedQuestionModel.class);
            return new ReviewQuestionVO(
                parsedQuestionId,
                question.getQuestionType(),
                question.getStem(),
                question.getOptions().stream()
                    .map(option -> new QuestionOptionVO(option.key(), option.text()))
                    .toList(),
                question.getAnswer(),
                question.getAnalysis(),
                question.getScore() == null ? BigDecimal.valueOf(5) : question.getScore(),
                question.getKnowledgePoint(),
                question.getDifficulty(),
                reviewStatus,
                reviewComment
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("题目 JSON 解析失败", exception);
        }
    }

    public StructuredParseResponse toStructuredResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode questions = root.path("questions");
            if (questions.isArray() && questions.size() > 0 && questions.get(0).has("t")) {
                AiImportResponse aiResponse = objectMapper.treeToValue(root, AiImportResponse.class);
                return toStructuredResponse(aiResponse);
            }
            return objectMapper.treeToValue(root, StructuredParseResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 返回的 JSON 无法解析", exception);
        }
    }

    public StructuredParseResponse toStructuredResponse(AiImportResponse aiResponse) {
        StructuredParseResponse response = new StructuredParseResponse();
        List<ParsedQuestionModel> questions = aiResponse.getQuestions() == null
            ? List.of()
            : aiResponse.getQuestions().stream()
                .map(this::toParsedQuestion)
                .toList();
        response.setQuestions(questions);
        return response;
    }

    private ParsedQuestionModel toParsedQuestion(AiImportQuestionModel aiQuestion) {
        ParsedQuestionModel question = new ParsedQuestionModel();
        question.setQuestionType(toInternalType(aiQuestion.getType()));
        question.setStem(aiQuestion.getContent());
        question.setOptions(toOptions(aiQuestion.getOptions()));
        question.setAnswer(toAnswerText(aiQuestion.getAnswer()));
        question.setAnalysis(aiQuestion.getExplanation());
        question.setScore(aiQuestion.getScore() == null ? defaultScore(aiQuestion.getType()) : aiQuestion.getScore());
        question.setKnowledgePoint("");
        question.setDifficulty("normal");
        return question;
    }

    private String toInternalType(String type) {
        return switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
            case "single" -> "single_choice";
            case "multi" -> "multiple_choice";
            case "judge" -> "true_false";
            case "blank" -> "fill_blank";
            case "essay" -> "subjective";
            default -> type;
        };
    }

    private List<ParsedQuestionOptionModel> toOptions(List<String> options) {
        if (options == null) {
            return List.of();
        }
        List<ParsedQuestionOptionModel> result = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            result.add(new ParsedQuestionOptionModel(String.valueOf((char) ('A' + index)), options.get(index)));
        }
        return result;
    }

    private String toAnswerText(Object answer) {
        if (answer == null) {
            return "";
        }
        JsonNode node = objectMapper.valueToTree(answer);
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(item -> values.add(item.asText()));
            return String.join(",", values);
        }
        return node.asText("");
    }

    private BigDecimal defaultScore(String type) {
        return switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
            case "single" -> BigDecimal.valueOf(2);
            case "multi" -> BigDecimal.valueOf(4);
            case "judge" -> BigDecimal.ONE;
            case "blank" -> BigDecimal.valueOf(3);
            case "essay" -> BigDecimal.TEN;
            default -> BigDecimal.valueOf(5);
        };
    }

    public String toRequestPayload(List<String> blocks) {
        try {
            return objectMapper.writeValueAsString(blocks);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("解析请求 JSON 序列化失败", exception);
        }
    }
}
