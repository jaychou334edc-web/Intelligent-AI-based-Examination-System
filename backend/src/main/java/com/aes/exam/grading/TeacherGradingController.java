package com.aes.exam.grading;

import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.grading.dto.GradeAnswerRequest;
import com.aes.exam.grading.service.GradingService;
import com.aes.exam.grading.vo.SubmissionGradingVO;
import com.aes.exam.grading.vo.SubmissionSummaryVO;
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

@Tag(name = "教师阅卷")
@RestController
@RequestMapping("/api/teacher/grading")
public class TeacherGradingController {

    private final GradingService gradingService;

    public TeacherGradingController(GradingService gradingService) {
        this.gradingService = gradingService;
    }

    @Operation(summary = "提交记录列表")
    @GetMapping("/submissions")
    public ApiResponse<List<SubmissionSummaryVO>> submissions() {
        return ApiResponse.success(gradingService.teacherSubmissions());
    }

    @Operation(summary = "提交记录详情")
    @GetMapping("/submissions/{submissionId}")
    public ApiResponse<SubmissionGradingVO> detail(@PathVariable Long submissionId) {
        return ApiResponse.success(gradingService.teacherSubmission(submissionId));
    }

    @Operation(summary = "人工评分")
    @PostMapping("/submissions/{submissionId}/answers")
    public ApiResponse<SubmissionGradingVO> gradeAnswer(
        @PathVariable Long submissionId,
        @Valid @RequestBody GradeAnswerRequest request
    ) {
        return ApiResponse.success(gradingService.gradeAnswer(submissionId, request));
    }
}
