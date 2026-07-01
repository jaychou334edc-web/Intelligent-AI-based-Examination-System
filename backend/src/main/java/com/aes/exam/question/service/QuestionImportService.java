package com.aes.exam.question.service;

import com.aes.exam.ai.repository.AiParsedQuestionRepository;
import com.aes.exam.ai.service.QuestionJsonMapper;
import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.BusinessException;
import com.aes.exam.common.security.SecurityContext;
import com.aes.exam.common.security.SecurityContextHolder;
import com.aes.exam.paper.service.PaperService;
import com.aes.exam.question.dto.ImportQuestionsRequest;
import com.aes.exam.question.dto.ReviewQuestionRequest;
import com.aes.exam.question.repository.QuestionRepository;
import com.aes.exam.question.vo.ImportQuestionsResultVO;
import com.aes.exam.question.vo.QuestionBankItemVO;
import com.aes.exam.question.vo.QuestionOptionVO;
import com.aes.exam.question.vo.ReviewQuestionVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionImportService {

    private final PaperService paperService;
    private final QuestionRepository questionRepository;
    private final AiParsedQuestionRepository parsedQuestionRepository;
    private final QuestionJsonMapper questionJsonMapper;

    public QuestionImportService(
        PaperService paperService,
        QuestionRepository questionRepository,
        AiParsedQuestionRepository parsedQuestionRepository,
        QuestionJsonMapper questionJsonMapper
    ) {
        this.paperService = paperService;
        this.questionRepository = questionRepository;
        this.parsedQuestionRepository = parsedQuestionRepository;
        this.questionJsonMapper = questionJsonMapper;
    }

    @Transactional
    public ImportQuestionsResultVO importQuestions(ImportQuestionsRequest request) {
        paperService.getRequired(request.paperId());
        SecurityContext context = SecurityContextHolder.current();
        if (context == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        int importedCount = 0;
        for (ReviewQuestionRequest question : request.questions()) {
            Long questionId = questionRepository.createQuestion(
                request.paperId(),
                question.parsedQuestionId(),
                question.questionType(),
                question.stem(),
                question.analysis(),
                question.score(),
                question.difficulty(),
                question.knowledgePoint(),
                context.userId()
            );
            questionRepository.createOptions(questionId, question.options(), question.answer());
            questionRepository.createAnswer(questionId, question.answer());

            if (question.parsedQuestionId() != null) {
                parsedQuestionRepository.markReviewed(
                    question.parsedQuestionId(),
                    "approved",
                    question.reviewComment(),
                    questionJsonMapper.toJson(toReviewVO(question))
                );
            }
            importedCount++;
        }

        return new ImportQuestionsResultVO(request.paperId(), importedCount);
    }

    public List<QuestionBankItemVO> findRecentQuestions(int limit) {
        return questionRepository.findRecent(limit);
    }

    @Transactional
    public QuestionBankItemVO updateQuestion(Long questionId, ReviewQuestionRequest request) {
        SecurityContext context = requireCurrentUser();
        ensureQuestionExists(questionId);
        questionRepository.updateQuestion(questionId, request, context.userId());
        return questionRepository.findById(questionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "题目不存在"));
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        SecurityContext context = requireCurrentUser();
        ensureQuestionExists(questionId);
        if (questionRepository.isUsedInExam(questionId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该题目已被考试引用，不能直接删除");
        }
        questionRepository.softDelete(questionId, context.userId());
    }

    private SecurityContext requireCurrentUser() {
        SecurityContext context = SecurityContextHolder.current();
        if (context == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return context;
    }

    private void ensureQuestionExists(Long questionId) {
        if (!questionRepository.existsActive(questionId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
    }

    private ReviewQuestionVO toReviewVO(ReviewQuestionRequest request) {
        List<QuestionOptionVO> options = request.options() == null
            ? List.of()
            : request.options().stream()
                .map(option -> new QuestionOptionVO(option.key(), option.text()))
                .toList();
        return new ReviewQuestionVO(
            request.parsedQuestionId(),
            request.questionType(),
            request.stem(),
            options,
            request.answer(),
            request.analysis(),
            request.score(),
            request.knowledgePoint(),
            request.difficulty(),
            "approved",
            request.reviewComment()
        );
    }
}
