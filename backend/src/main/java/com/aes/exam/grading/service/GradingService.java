package com.aes.exam.grading.service;

import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.BusinessException;
import com.aes.exam.common.security.SecurityContext;
import com.aes.exam.common.security.SecurityContextHolder;
import com.aes.exam.grading.dto.GradeAnswerRequest;
import com.aes.exam.grading.repository.GradingRepository;
import com.aes.exam.grading.vo.SubmissionGradingVO;
import com.aes.exam.grading.vo.SubmissionSummaryVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingService {

    private final GradingRepository gradingRepository;

    public GradingService(GradingRepository gradingRepository) {
        this.gradingRepository = gradingRepository;
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
}
