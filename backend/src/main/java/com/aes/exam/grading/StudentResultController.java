package com.aes.exam.grading;

import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.grading.service.GradingService;
import com.aes.exam.grading.vo.SubmissionGradingVO;
import com.aes.exam.grading.vo.SubmissionSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "学生成绩")
@RestController
@RequestMapping("/api/student/results")
public class StudentResultController {

    private final GradingService gradingService;

    public StudentResultController(GradingService gradingService) {
        this.gradingService = gradingService;
    }

    @Operation(summary = "我的成绩列表")
    @GetMapping
    public ApiResponse<List<SubmissionSummaryVO>> results() {
        return ApiResponse.success(gradingService.studentResults());
    }

    @Operation(summary = "成绩详情")
    @GetMapping("/{submissionId}")
    public ApiResponse<SubmissionGradingVO> detail(@PathVariable Long submissionId) {
        return ApiResponse.success(gradingService.studentResult(submissionId));
    }
}
