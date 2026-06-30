package com.aes.exam.ai.service;

import java.util.ArrayList;
import java.util.List;

public class AiImportResponse {

    private List<AiImportQuestionModel> questions = new ArrayList<>();

    public List<AiImportQuestionModel> getQuestions() {
        return questions;
    }

    public void setQuestions(List<AiImportQuestionModel> questions) {
        this.questions = questions;
    }
}
