package com.aes.exam.question;

import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.question.dto.ImportQuestionsRequest;
import com.aes.exam.question.service.QuestionImportService;
import com.aes.exam.question.vo.ImportQuestionsResultVO;
import com.aes.exam.question.vo.QuestionBankItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "题库")
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionImportService questionImportService;

    public QuestionController(QuestionImportService questionImportService) {
        this.questionImportService = questionImportService;
    }

    @Operation(summary = "导入教师审核后的题目")
    @PostMapping("/import")
    public ApiResponse<ImportQuestionsResultVO> importQuestions(@Valid @RequestBody ImportQuestionsRequest request) {
        return ApiResponse.success(questionImportService.importQuestions(request));
    }

    @Operation(summary = "查看最近导入题库")
    @GetMapping
    public ApiResponse<List<QuestionBankItemVO>> recentQuestions(
        @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        return ApiResponse.success(questionImportService.findRecentQuestions(limit));
    }
}
