package com.aes.exam.grading.service;

import com.aes.exam.ai.service.DeepSeekClient;
import com.aes.exam.common.config.AesProperties;
import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.BusinessException;
import com.aes.exam.common.security.SecurityContext;
import com.aes.exam.common.security.SecurityContextHolder;
import com.aes.exam.grading.dto.GradeAnswerRequest;
import com.aes.exam.grading.repository.GradingRepository;
import com.aes.exam.grading.vo.AiGradeSuggestionVO;
import com.aes.exam.grading.vo.GradingAnswerVO;
import com.aes.exam.grading.vo.SubmissionGradingVO;
import com.aes.exam.grading.vo.SubmissionSummaryVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GradingService {

    private final GradingRepository gradingRepository;
    private final DeepSeekClient deepSeekClient;
    private final AesProperties aesProperties;
    private final ObjectMapper objectMapper;

    public GradingService(
        GradingRepository gradingRepository,
        DeepSeekClient deepSeekClient,
        AesProperties aesProperties,
        ObjectMapper objectMapper
    ) {
        this.gradingRepository = gradingRepository;
        this.deepSeekClient = deepSeekClient;
        this.aesProperties = aesProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void gradeSubmittedSubmission(Long submissionId) {
        gradingRepository.gradeSubmittedObjectiveAnswers(submissionId);
    }

    public List<SubmissionSummaryVO> teacherSubmissions() {
        return gradingRepository.findTeacherSubmissions(currentUser().userId());
    }

    public SubmissionGradingVO teacherSubmission(Long submissionId) {
        return gradingRepository.findTeacherSubmission(submissionId, currentUser().userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "提交记录不存在"));
    }

    @Transactional
    public SubmissionGradingVO gradeAnswer(Long submissionId, GradeAnswerRequest request) {
        SecurityContext context = currentUser();
        SubmissionGradingVO submission = gradingRepository.findTeacherSubmission(submissionId, context.userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "提交记录不存在"));
        boolean containsQuestion = submission.answers().stream()
            .anyMatch(answer -> answer.questionId().equals(request.questionId()));
        if (!containsQuestion) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "题目不属于当前提交记录");
        }
        try {
            gradingRepository.updateManualGrade(
                submissionId,
                request.questionId(),
                request.score(),
                request.teacherComment(),
                context.userId()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, exception.getMessage());
        }
        return teacherSubmission(submissionId);
    }

    @Transactional
    public SubmissionGradingVO suggestAiGrade(Long submissionId, Long questionId) {
        SecurityContext context = currentUser();
        SubmissionGradingVO submission = gradingRepository.findTeacherSubmission(submissionId, context.userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "提交记录不存在"));
        GradingAnswerVO answer = submission.answers().stream()
            .filter(value -> value.questionId().equals(questionId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "题目不属于当前提交记录"));
        if (!isSubjectiveLike(answer)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅主观题、代码题和人工题支持 AI 辅助评分");
        }

        AiGradeSuggestionVO suggestion = generateSuggestion(submission, answer);
        gradingRepository.updateAiSuggestion(
            submissionId,
            questionId,
            suggestion.suggestedScore(),
            suggestion.reason()
        );
        return teacherSubmission(submissionId);
    }

    public List<SubmissionSummaryVO> studentResults() {
        return gradingRepository.findStudentSubmissions(currentUser().userId());
    }

    public SubmissionGradingVO studentResult(Long submissionId) {
        return gradingRepository.findStudentSubmission(submissionId, currentUser().userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "成绩记录不存在"));
    }

    private SecurityContext currentUser() {
        SecurityContext context = SecurityContextHolder.current();
        if (context == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return context;
    }

    private boolean isSubjectiveLike(GradingAnswerVO answer) {
        return "subjective".equals(answer.questionType())
            || "fill_blank".equals(answer.questionType())
            || "pending".equals(answer.gradingStatus());
    }

    private AiGradeSuggestionVO generateSuggestion(SubmissionGradingVO submission, GradingAnswerVO answer) {
        if (aesProperties.getAi().isMockEnabled() || !StringUtils.hasText(aesProperties.getAi().getDeepseekApiKey())) {
            return mockSuggestion(submission.submissionId(), answer);
        }
        try {
            String response = deepSeekClient.chatJson(systemPrompt(), userPrompt(submission, answer), 0.2, Duration.ofSeconds(90));
            JsonNode root = objectMapper.readTree(response);
            BigDecimal score = new BigDecimal(root.path("score").asText("0"))
                .max(BigDecimal.ZERO)
                .min(answer.maxScore())
                .setScale(1, RoundingMode.HALF_UP);
            String reason = root.path("reason").asText("AI 给出了评分建议，请教师复核。");
            return new AiGradeSuggestionVO(submission.submissionId(), answer.questionId(), score, reason);
        } catch (Exception exception) {
            return mockSuggestion(submission.submissionId(), answer);
        }
    }

    private AiGradeSuggestionVO mockSuggestion(Long submissionId, GradingAnswerVO answer) {
        String studentAnswer = answer.studentAnswer() == null ? "" : answer.studentAnswer().trim();
        BigDecimal ratio;
        String reason;
        if (studentAnswer.isBlank()) {
            ratio = BigDecimal.ZERO;
            reason = "学生未作答，建议给 0 分。";
        } else if (studentAnswer.length() >= 80) {
            ratio = new BigDecimal("0.85");
            reason = "答案较完整，覆盖了主要论述点；建议教师重点检查概念准确性和代码规范。";
        } else if (studentAnswer.length() >= 30) {
            ratio = new BigDecimal("0.65");
            reason = "答案有基本思路，但展开不够充分；建议教师根据关键点覆盖情况微调。";
        } else {
            ratio = new BigDecimal("0.40");
            reason = "答案较短，可能只覆盖部分要点；建议教师结合标准答案复核。";
        }
        BigDecimal score = answer.maxScore().multiply(ratio).setScale(1, RoundingMode.HALF_UP);
        return new AiGradeSuggestionVO(submissionId, answer.questionId(), score, reason);
    }

    private String systemPrompt() {
        return """
            你是在线考试系统的主观题辅助评分助手。
            你只输出 JSON，不要 Markdown，不要解释。
            输出格式：{"score": 数字, "reason": "评分理由"}。
            score 必须在 0 到题目满分之间，可以保留 1 位小数。
            你的结果只是教师参考，理由要具体指出得分依据、缺失点和是否需要教师复核。
            """;
    }

    private String userPrompt(SubmissionGradingVO submission, GradingAnswerVO answer) {
        return """
            考试：%s
            学生：%s
            题型：%s
            满分：%s
            题干：
            %s

            参考答案或评分依据：
            %s

            学生答案：
            %s
            """.formatted(
            submission.examTitle(),
            submission.studentName(),
            answer.questionType(),
            answer.maxScore(),
            answer.stem(),
            answer.correctAnswer() == null ? "无明确参考答案，请依据题干和答案质量评分。" : answer.correctAnswer(),
            answer.studentAnswer() == null ? "" : answer.studentAnswer()
        );
    }
}
