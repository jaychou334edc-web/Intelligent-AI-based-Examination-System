package com.aes.exam.monitoring;

import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.monitoring.dto.AntiCheatEventRequest;
import com.aes.exam.monitoring.service.MonitoringService;
import com.aes.exam.monitoring.vo.EventRecordedVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "学生考试行为")
@RestController
@RequestMapping("/api/student/monitoring")
public class StudentMonitoringController {

    private final MonitoringService monitoringService;

    public StudentMonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Operation(summary = "记录考试行为事件")
    @PostMapping("/exams/{examId}/events")
    public ApiResponse<EventRecordedVO> recordEvent(
        @PathVariable Long examId,
        @Valid @RequestBody AntiCheatEventRequest request
    ) {
        return ApiResponse.success(monitoringService.recordStudentEvent(examId, request));
    }
}
