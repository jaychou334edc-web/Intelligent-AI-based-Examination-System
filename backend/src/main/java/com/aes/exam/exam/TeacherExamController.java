package com.aes.exam.exam;

import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.exam.dto.CreateExamRequest;
import com.aes.exam.exam.dto.UpdateExamRequest;
import com.aes.exam.exam.dto.UpdateExamQuestionsRequest;
import com.aes.exam.exam.service.ExamService;
import com.aes.exam.exam.vo.ExamActionVO;
import com.aes.exam.exam.vo.ExamDetailVO;
import com.aes.exam.exam.vo.ExamSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "教师考试管理")
@RestController
@RequestMapping("/api/teacher/exams")
public class TeacherExamController {

    private final ExamService examService;

    public TeacherExamController(ExamService examService) {
        this.examService = examService;
    }

    @Operation(summary = "创建考试草稿")
    @PostMapping
    public ApiResponse<ExamDetailVO> create(@Valid @RequestBody CreateExamRequest request) {
        return ApiResponse.success(examService.create(request));
    }

    @Operation(summary = "教师考试列表")
    @GetMapping
    public ApiResponse<List<ExamSummaryVO>> list() {
        return ApiResponse.success(examService.teacherExams());
    }

    @Operation(summary = "教师考试详情")
    @GetMapping("/{examId}")
    public ApiResponse<ExamDetailVO> detail(@PathVariable Long examId) {
        return ApiResponse.success(examService.teacherDetail(examId));
    }

    @Operation(summary = "修改考试草稿")
    @PutMapping("/{examId}")
    public ApiResponse<ExamDetailVO> update(
        @PathVariable Long examId,
        @Valid @RequestBody UpdateExamRequest request
    ) {
        return ApiResponse.success(examService.update(examId, request));
    }

    @Operation(summary = "删除草稿或归档已发布考试")
    @DeleteMapping("/{examId}")
    public ApiResponse<ExamActionVO> deleteOrArchive(@PathVariable Long examId) {
        examService.deleteOrArchive(examId);
        return ApiResponse.success(new ExamActionVO(examId, "ok"));
    }

    @Operation(summary = "设置考试题目")
    @PostMapping("/{examId}/questions")
    public ApiResponse<ExamDetailVO> questions(
        @PathVariable Long examId,
        @Valid @RequestBody UpdateExamQuestionsRequest request
    ) {
        return ApiResponse.success(examService.replaceQuestions(examId, request));
    }

    @Operation(summary = "发布考试")
    @PostMapping("/{examId}/publish")
    public ApiResponse<ExamDetailVO> publish(@PathVariable Long examId) {
        return ApiResponse.success(examService.publish(examId));
    }
}
