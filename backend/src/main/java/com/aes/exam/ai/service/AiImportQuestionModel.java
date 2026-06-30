package com.aes.exam.ai.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AiImportQuestionModel {

    @JsonProperty("t")
    private String type;

    @JsonProperty("c")
    private String content;

    @JsonProperty("o")
    private List<String> options = new ArrayList<>();

    @JsonProperty("a")
    private Object answer;

    @JsonProperty("e")
    private String explanation = "";

    @JsonProperty("s")
    private BigDecimal score;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public Object getAnswer() {
        return answer;
    }

    public void setAnswer(Object answer) {
        this.answer = answer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }
}
