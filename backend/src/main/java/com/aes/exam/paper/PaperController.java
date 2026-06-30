package com.aes.exam.paper;

import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.paper.service.PaperService;
import com.aes.exam.paper.vo.PaperVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "题库源文件上传")
@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @Operation(summary = "上传题库源文件")
    @PostMapping
    public ApiResponse<PaperVO> upload(
        @RequestPart("file") MultipartFile file,
        @RequestParam(value = "title", required = false) String title
    ) {
        return ApiResponse.success(paperService.upload(file, title));
    }
}
