package com.aes.exam.ai.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ParsedQuestionModel {

    private String questionType;
    private String stem;
    private List<ParsedQuestionOptionModel> options = new ArrayList<>();
    private String answer;
    private String analysis = "";
    private BigDecimal score = BigDecimal.valueOf(5);
    private String knowledgePoint;
    private String difficulty = "normal";

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getStem() {
        return stem;
    }

    public void setStem(String stem) {
        this.stem = stem;
    }

    public List<ParsedQuestionOptionModel> getOptions() {
        return options;
    }

    public void setOptions(List<ParsedQuestionOptionModel> options) {
        this.options = options;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getKnowledgePoint() {
        return knowledgePoint;
    }

    public void setKnowledgePoint(String knowledgePoint) {
        this.knowledgePoint = knowledgePoint;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
