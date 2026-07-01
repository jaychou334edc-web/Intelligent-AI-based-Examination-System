package com.aes.exam.exam.repository;

import com.aes.exam.exam.dto.SaveAnswerRequest;
import com.aes.exam.exam.vo.ExamDetailVO;
import com.aes.exam.exam.vo.ExamQuestionVO;
import com.aes.exam.exam.vo.ExamSummaryVO;
import com.aes.exam.exam.vo.SaveAnswerVO;
import com.aes.exam.exam.vo.SubmitExamVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamRepository {

    Long create(String title, String description, Integer durationMinutes, Long currentUserId);

    void updateDraft(Long examId, String title, String description, Integer durationMinutes, Long teacherId);

    List<ExamSummaryVO> findTeacherExams(Long teacherId);

    Optional<ExamDetailVO> findTeacherExamDetail(Long examId, Long teacherId);

    boolean isDraftOwnedByTeacher(Long examId, Long teacherId);

    void replaceQuestions(Long examId, List<Long> questionIds);

    void publish(Long examId, Long teacherId);

    void deleteDraft(Long examId, Long teacherId);

    void archivePublished(Long examId, Long teacherId);

    void assignPublishedExamToStudents(Long examId);

    List<ExamSummaryVO> findStudentExams(Long studentId);

    Optional<ExamDetailVO> findStudentExamDetail(Long examId, Long studentId);

    Long ensureSubmission(Long examId, Long studentId);

    boolean isQuestionInExam(Long examId, Long questionId);

    SaveAnswerVO saveAnswer(Long submissionId, Long examId, SaveAnswerRequest request);

    SubmitExamVO submit(Long submissionId, Long examId, List<SaveAnswerRequest> answers);

    SubmitExamVO findSubmitResult(Long submissionId);
}
