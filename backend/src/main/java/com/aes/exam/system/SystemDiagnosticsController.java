package com.aes.exam.system;

import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.sample.dto.SampleUserCreateRequest;
import com.aes.exam.sample.mapper.SampleUserMapper;
import com.aes.exam.sample.vo.SampleUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工程诊断")
@RestController
@RequestMapping("/api/system/diagnostics")
public class SystemDiagnosticsController {

    private final SampleUserMapper mapper;

    public SystemDiagnosticsController(SampleUserMapper mapper) {
        this.mapper = mapper;
    }

    @Operation(summary = "验证 Bean Validation 与 MapStruct 映射")
    @PostMapping("/mapping")
    public ApiResponse<SampleUserVO> validateAndMap(@Valid @RequestBody SampleUserCreateRequest request) {
        return ApiResponse.success(mapper.toVo(mapper.toEntity(request)));
    }
}
