package com.aes.exam.exam;

import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.exam.dto.SaveAnswerRequest;
import com.aes.exam.exam.dto.SubmitExamRequest;
import com.aes.exam.exam.service.ExamService;
import com.aes.exam.exam.vo.ExamDetailVO;
import com.aes.exam.exam.vo.ExamSummaryVO;
import com.aes.exam.exam.vo.SaveAnswerVO;
import com.aes.exam.exam.vo.SubmitExamVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "学生考试作答")
@RestController
@RequestMapping("/api/student/exams")
public class StudentExamController {

    private final ExamService examService;

    public StudentExamController(ExamService examService) {
        this.examService = examService;
    }

    @Operation(summary = "学生考试列表")
    @GetMapping
    public ApiResponse<List<ExamSummaryVO>> list() {
        return ApiResponse.success(examService.studentExams());
    }

    @Operation(summary = "进入考试")
    @GetMapping("/{examId}")
    public ApiResponse<ExamDetailVO> detail(@PathVariable Long examId) {
        return ApiResponse.success(examService.startStudentExam(examId));
    }

    @Operation(summary = "保存单题答案")
    @PostMapping("/{examId}/answers")
    public ApiResponse<SaveAnswerVO> saveAnswer(
        @PathVariable Long examId,
        @Valid @RequestBody SaveAnswerRequest request
    ) {
        return ApiResponse.success(examService.saveAnswer(examId, request));
    }

    @Operation(summary = "提交考试")
    @PostMapping("/{examId}/submit")
    public ApiResponse<SubmitExamVO> submit(
        @PathVariable Long examId,
        @RequestBody SubmitExamRequest request
    ) {
        return ApiResponse.success(examService.submit(examId, request));
    }
}
