package com.aes.exam.exam.service;

import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.BusinessException;
import com.aes.exam.common.security.SecurityContext;
import com.aes.exam.common.security.SecurityContextHolder;
import com.aes.exam.exam.dto.CreateExamRequest;
import com.aes.exam.exam.dto.SaveAnswerRequest;
import com.aes.exam.exam.dto.SubmitExamRequest;
import com.aes.exam.exam.dto.UpdateExamRequest;
import com.aes.exam.exam.dto.UpdateExamQuestionsRequest;
import com.aes.exam.exam.repository.ExamRepository;
import com.aes.exam.exam.vo.ExamDetailVO;
import com.aes.exam.exam.vo.ExamSummaryVO;
import com.aes.exam.exam.vo.SaveAnswerVO;
import com.aes.exam.exam.vo.SubmitExamVO;
import com.aes.exam.grading.service.GradingService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final GradingService gradingService;

    public ExamService(ExamRepository examRepository, GradingService gradingService) {
        this.examRepository = examRepository;
        this.gradingService = gradingService;
    }

    @Transactional
    public ExamDetailVO create(CreateExamRequest request) {
        SecurityContext context = currentUser();
        Long examId = examRepository.create(
            request.title().trim(),
            StringUtils.hasText(request.description()) ? request.description().trim() : null,
            request.durationMinutes(),
            context.userId()
        );
        return teacherDetail(examId);
    }

    public List<ExamSummaryVO> teacherExams() {
        return examRepository.findTeacherExams(currentUser().userId());
    }

    @Transactional
    public ExamDetailVO update(Long examId, UpdateExamRequest request) {
        SecurityContext context = currentUser();
        if (!examRepository.isDraftOwnedByTeacher(examId, context.userId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有草稿考试可以修改基本信息");
        }
        examRepository.updateDraft(
            examId,
            request.title().trim(),
            StringUtils.hasText(request.description()) ? request.description().trim() : null,
            request.durationMinutes(),
            context.userId()
        );
        return teacherDetail(examId);
    }

    public ExamDetailVO teacherDetail(Long examId) {
        return examRepository.findTeacherExamDetail(examId, currentUser().userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "考试不存在"));
    }

    @Transactional
    public void deleteOrArchive(Long examId) {
        SecurityContext context = currentUser();
        ExamDetailVO detail = teacherDetail(examId);
        if ("draft".equals(detail.status())) {
            examRepository.deleteDraft(examId, context.userId());
            return;
        }
        if ("published".equals(detail.status())) {
            examRepository.archivePublished(examId, context.userId());
            return;
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "考试已经归档");
    }

    @Transactional
    public ExamDetailVO replaceQuestions(Long examId, UpdateExamQuestionsRequest request) {
        SecurityContext context = currentUser();
        if (!examRepository.isDraftOwnedByTeacher(examId, context.userId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有草稿考试可以调整题目");
        }
        List<Long> questionIds = request.questionIds().stream().distinct().toList();
        examRepository.replaceQuestions(examId, questionIds);
        ExamDetailVO detail = teacherDetail(examId);
        if (detail.questionCount() != questionIds.size()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "部分题目不存在或不可用");
        }
        return detail;
    }

    @Transactional
    public ExamDetailVO publish(Long examId) {
        SecurityContext context = currentUser();
        ExamDetailVO detail = teacherDetail(examId);
        if (!"draft".equals(detail.status())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "考试已经发布");
        }
        if (detail.questionCount() == null || detail.questionCount() == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先为考试选择题目");
        }
        examRepository.publish(examId, context.userId());
        examRepository.assignPublishedExamToStudents(examId);
        return teacherDetail(examId);
    }

    public List<ExamSummaryVO> studentExams() {
        return examRepository.findStudentExams(currentUser().userId());
    }

    @Transactional
    public ExamDetailVO startStudentExam(Long examId) {
        SecurityContext context = currentUser();
        ExamDetailVO detail = examRepository.findStudentExamDetail(examId, context.userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "考试不存在或尚未发布"));
        Long submissionId = examRepository.ensureSubmission(examId, context.userId());
        return examRepository.findStudentExamDetail(examId, context.userId())
            .map(value -> new ExamDetailVO(
                value.id(),
                value.title(),
                value.description(),
                value.durationMinutes(),
                value.status(),
                value.questionCount(),
                value.totalScore(),
                value.publishedAt(),
                value.questions(),
                submissionId,
                value.submissionStatus(),
                value.submissionStartedAt(),
                value.remainingSeconds()
            ))
            .orElse(detail);
    }

    @Transactional
    public SaveAnswerVO saveAnswer(Long examId, SaveAnswerRequest request) {
        SecurityContext context = currentUser();
        ExamDetailVO detail = examRepository.findStudentExamDetail(examId, context.userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "考试不存在或尚未发布"));
        if ("submitted".equals(detail.submissionStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "考试已提交，不能继续修改答案");
        }
        if (!examRepository.isQuestionInExam(examId, request.questionId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "题目不属于当前考试");
        }
        Long submissionId = examRepository.ensureSubmission(examId, context.userId());
        return examRepository.saveAnswer(submissionId, examId, request);
    }

    @Transactional
    public SubmitExamVO submit(Long examId, SubmitExamRequest request) {
        SecurityContext context = currentUser();
        ExamDetailVO detail = examRepository.findStudentExamDetail(examId, context.userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "考试不存在或尚未发布"));
        if ("submitted".equals(detail.submissionStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "考试已提交");
        }
        Long submissionId = examRepository.ensureSubmission(examId, context.userId());
        List<SaveAnswerRequest> answers = request.answers() == null ? List.of() : request.answers();
        for (SaveAnswerRequest answer : answers) {
            if (!examRepository.isQuestionInExam(examId, answer.questionId())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "题目不属于当前考试");
            }
        }
        SubmitExamVO submitted = examRepository.submit(submissionId, examId, answers);
        gradingService.gradeSubmittedSubmission(submissionId);
        return examRepository.findSubmitResult(submitted.submissionId());
    }

    private SecurityContext currentUser() {
        SecurityContext context = SecurityContextHolder.current();
        if (context == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return context;
    }
}
