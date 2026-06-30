package com.aes.exam.ai.service;

import java.util.ArrayList;
import java.util.List;

public class StructuredParseResponse {

    private List<ParsedQuestionModel> questions = new ArrayList<>();

    public List<ParsedQuestionModel> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ParsedQuestionModel> questions) {
        this.questions = questions;
    }
}
