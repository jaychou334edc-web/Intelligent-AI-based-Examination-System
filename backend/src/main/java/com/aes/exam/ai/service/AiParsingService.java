package com.aes.exam.ai.service;

import com.aes.exam.ai.entity.AiParsedQuestionEntity;
import com.aes.exam.ai.repository.AiLogRepository;
import com.aes.exam.ai.repository.AiParseJobRepository;
import com.aes.exam.ai.repository.AiParsedQuestionRepository;
import com.aes.exam.ai.vo.ParseResultVO;
import com.aes.exam.common.config.AesProperties;
import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.BusinessException;
import com.aes.exam.paper.entity.PaperEntity;
import com.aes.exam.paper.repository.PaperRepository;
import com.aes.exam.paper.service.DocxExtractionService;
import com.aes.exam.paper.service.DocxExtractionService.ExtractedDocument;
import com.aes.exam.paper.service.PaperService;
import com.aes.exam.question.vo.ReviewQuestionVO;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiParsingService {

    private final AesProperties properties;
    private final PaperService paperService;
    private final PaperRepository paperRepository;
    private final DocxExtractionService docxExtractionService;
    private final AiPromptBuilder promptBuilder;
    private final DeepSeekClient deepSeekClient;
    private final QuestionChunker questionChunker;
    private final RuleBasedQuestionParser ruleBasedQuestionParser;
    private final QuestionValidationService validationService;
    private final QuestionJsonMapper questionJsonMapper;
    private final AiParseJobRepository jobRepository;
    private final AiParsedQuestionRepository parsedQuestionRepository;
    private final AiLogRepository aiLogRepository;

    public AiParsingService(
        AesProperties properties,
        PaperService paperService,
        PaperRepository paperRepository,
        DocxExtractionService docxExtractionService,
        AiPromptBuilder promptBuilder,
        DeepSeekClient deepSeekClient,
        QuestionChunker questionChunker,
        RuleBasedQuestionParser ruleBasedQuestionParser,
        QuestionValidationService validationService,
        QuestionJsonMapper questionJsonMapper,
        AiParseJobRepository jobRepository,
        AiParsedQuestionRepository parsedQuestionRepository,
        AiLogRepository aiLogRepository
    ) {
        this.properties = properties;
        this.paperService = paperService;
        this.paperRepository = paperRepository;
        this.docxExtractionService = docxExtractionService;
        this.promptBuilder = promptBuilder;
        this.deepSeekClient = deepSeekClient;
        this.questionChunker = questionChunker;
        this.ruleBasedQuestionParser = ruleBasedQuestionParser;
        this.validationService = validationService;
        this.questionJsonMapper = questionJsonMapper;
        this.jobRepository = jobRepository;
        this.parsedQuestionRepository = parsedQuestionRepository;
        this.aiLogRepository = aiLogRepository;
    }

    @Transactional
    public ParseResultVO parsePaper(Long paperId) {
        PaperEntity paper = paperService.getRequired(paperId);
        String aiModel = properties.getAi().isMockEnabled() ? "mock-rule-parser" : properties.getAi().getModel();
        Long jobId = jobRepository.create(paperId, "running", "java", aiModel, LocalDateTime.now());

        try {
            ExtractedDocument document = docxExtractionService.extract(Path.of(paper.filePath()));
            String rawText = document.text();
            List<String> chunks = questionChunker.chunk(rawText);
            ParseOutcome outcome = parseWithConfiguredProvider(paper.title(), document, chunks);
            String requestPayload = outcome.requestPayload();
            StructuredParseResponse response = outcome.response();
            if (response.getQuestions().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "未能从 Word 中识别出题目");
            }

            parsedQuestionRepository.deleteByPaperId(paperId);
            for (ParsedQuestionModel question : response.getQuestions()) {
                validationService.validate(question);
                parsedQuestionRepository.create(
                    paperId,
                    jobId,
                    questionJsonMapper.toJson(question),
                    BigDecimal.valueOf(0.85),
                    "needs_review"
                );
            }

            String responsePayload = questionJsonMapper.toJson(response);
            paperRepository.updateRawTextAndStatus(paperId, rawText, "parsed", aiModel);
            jobRepository.complete(jobId, "succeeded", requestPayload, responsePayload, outcome.errorMessage(), LocalDateTime.now());
            aiLogRepository.create(
                paperId,
                jobId,
                requestPayload,
                outcome.providerResponsePayload() == null ? responsePayload : outcome.providerResponsePayload(),
                aiModel,
                outcome.provider()
            );
            return getParseResult(paperId);
        } catch (RuntimeException exception) {
            paperRepository.updateRawTextAndStatus(paperId, paper.rawText(), "failed", aiModel);
            jobRepository.complete(jobId, "failed", null, null, exception.getMessage(), LocalDateTime.now());
            throw exception;
        }
    }

    private ParseOutcome parseWithConfiguredProvider(String title, ExtractedDocument document, List<String> chunks) {
        String rawText = document.text();
        if (properties.getAi().isMockEnabled()) {
            return new ParseOutcome(
                ruleBasedQuestionParser.parse(ruleBasedQuestionParser.segment(rawText)),
                "mock",
                rawText,
                null,
                null
            );
        }

        String requestPayload = buildRequestPayload(title, document, chunks);
        try {
            StructuredParseResponse mergedResponse = new StructuredParseResponse();
            List<ParsedQuestionModel> questions = new ArrayList<>();
            StringBuilder responsePayload = new StringBuilder();
            String systemPrompt = promptBuilder.systemPrompt();
            for (int index = 0; index < chunks.size(); index++) {
                String prompt = promptBuilder.build(title, document, chunks.get(index), index + 1, chunks.size());
                String aiJson = deepSeekClient.parsePaper(systemPrompt, prompt);
                responsePayload
                    .append("chunk ")
                    .append(index + 1)
                    .append("/")
                    .append(chunks.size())
                    .append(":\n")
                    .append(aiJson)
                    .append("\n\n");
                questions.addAll(questionJsonMapper.toStructuredResponse(aiJson).getQuestions());
            }
            mergedResponse.setQuestions(questions);
            return new ParseOutcome(mergedResponse, "deepseek", requestPayload, responsePayload.toString().trim(), null);
        } catch (RuntimeException exception) {
            if (!properties.getAi().isFallbackToRuleParser()) {
                throw exception;
            }
            StructuredParseResponse fallbackResponse = ruleBasedQuestionParser.parse(ruleBasedQuestionParser.segment(rawText));
            return new ParseOutcome(fallbackResponse, "deepseek-fallback-rule", requestPayload, null, exception.getMessage());
        }
    }

    private String buildRequestPayload(String title, ExtractedDocument document, List<String> chunks) {
        StringBuilder requestPayload = new StringBuilder(promptBuilder.systemPrompt()).append("\n\n");
        for (int index = 0; index < chunks.size(); index++) {
            requestPayload
                .append(promptBuilder.build(title, document, chunks.get(index), index + 1, chunks.size()))
                .append("\n\n---\n\n");
        }
        return requestPayload.toString().trim();
    }

    public ParseResultVO getParseResult(Long paperId) {
        PaperEntity paper = paperService.getRequired(paperId);
        List<AiParsedQuestionEntity> parsedQuestions = parsedQuestionRepository.findByPaperId(paperId);
        List<ReviewQuestionVO> questions = parsedQuestions.stream()
            .map(question -> questionJsonMapper.toReviewQuestion(
                question.id(),
                question.questionJson(),
                question.reviewStatus(),
                question.reviewComment()
            ))
            .toList();
        Long parseJobId = parsedQuestions.isEmpty() ? null : parsedQuestions.get(0).parseJobId();
        return new ParseResultVO(paperId, parseJobId, paper.parseStatus(), paper.rawText(), questions);
    }

    private record ParseOutcome(
        StructuredParseResponse response,
        String provider,
        String requestPayload,
        String providerResponsePayload,
        String errorMessage
    ) {
    }
}
