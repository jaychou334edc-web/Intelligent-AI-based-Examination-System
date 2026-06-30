package com.aes.exam.paper;

import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.paper.service.PaperImageService;
import com.aes.exam.paper.service.PaperImageService.PaperImageResource;
import com.aes.exam.paper.service.PaperService;
import com.aes.exam.paper.vo.PaperVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final PaperImageService paperImageService;

    public PaperController(PaperService paperService, PaperImageService paperImageService) {
        this.paperService = paperService;
        this.paperImageService = paperImageService;
    }

    @Operation(summary = "上传题库源文件")
    @PostMapping
    public ApiResponse<PaperVO> upload(
        @RequestPart("file") MultipartFile file,
        @RequestParam(value = "title", required = false) String title
    ) {
        return ApiResponse.success(paperService.upload(file, title));
    }

    @Operation(summary = "读取题目图片")
    @GetMapping("/{paperId}/images/{imageId}")
    public ResponseEntity<Resource> image(@PathVariable Long paperId, @PathVariable String imageId) {
        PaperImageResource image = paperImageService.getImage(paperId, imageId);
        return ResponseEntity.ok()
            .contentType(image.mediaType())
            .body(image.resource());
    }
}
