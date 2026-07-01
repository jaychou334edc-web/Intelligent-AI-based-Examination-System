package com.aes.exam.grading.repository;

import com.aes.exam.grading.vo.GradingAnswerVO;
import com.aes.exam.grading.vo.SubmissionGradingVO;
import com.aes.exam.grading.vo.SubmissionSummaryVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface GradingRepository {

    void gradeSubmittedObjectiveAnswers(Long submissionId);

    List<SubmissionSummaryVO> findTeacherSubmissions(Long teacherId);

    List<SubmissionSummaryVO> findStudentSubmissions(Long studentId);

    Optional<SubmissionGradingVO> findTeacherSubmission(Long submissionId, Long teacherId);

    Optional<SubmissionGradingVO> findStudentSubmission(Long submissionId, Long studentId);

    void updateManualGrade(Long submissionId, Long questionId, BigDecimal score, String teacherComment, Long graderId);

    void updateAiSuggestion(Long submissionId, Long questionId, BigDecimal score, String comment);

    void refreshSubmissionTotal(Long submissionId);
}
