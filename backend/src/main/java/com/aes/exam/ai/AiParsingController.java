package com.aes.exam.ai;

import com.aes.exam.ai.dto.ParsePaperRequest;
import com.aes.exam.ai.service.AiParsingService;
import com.aes.exam.ai.vo.ParseResultVO;
import com.aes.exam.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 试卷解析")
@RestController
@RequestMapping("/api/ai")
public class AiParsingController {

    private final AiParsingService aiParsingService;

    public AiParsingController(AiParsingService aiParsingService) {
        this.aiParsingService = aiParsingService;
    }

    @Operation(summary = "解析已上传试卷")
    @PostMapping("/parse-paper")
    public ApiResponse<ParseResultVO> parsePaper(@Valid @RequestBody ParsePaperRequest request) {
        return ApiResponse.success(aiParsingService.parsePaper(request.paperId()));
    }

    @Operation(summary = "获取试卷解析结果")
    @GetMapping("/parse-result/{paperId}")
    public ApiResponse<ParseResultVO> parseResult(@PathVariable Long paperId) {
        return ApiResponse.success(aiParsingService.getParseResult(paperId));
    }
}
