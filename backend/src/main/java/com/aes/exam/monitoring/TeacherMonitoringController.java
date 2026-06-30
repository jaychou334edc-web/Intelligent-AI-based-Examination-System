package com.aes.exam.monitoring;

import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.monitoring.service.MonitoringService;
import com.aes.exam.monitoring.vo.AntiCheatEventVO;
import com.aes.exam.monitoring.vo.ExamAnalyticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "教师考试监控分析")
@RestController
@RequestMapping("/api/teacher/monitoring")
public class TeacherMonitoringController {

    private final MonitoringService monitoringService;

    public TeacherMonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Operation(summary = "考试行为时间线")
    @GetMapping("/exams/{examId}/events")
    public ApiResponse<List<AntiCheatEventVO>> events(@PathVariable Long examId) {
        return ApiResponse.success(monitoringService.teacherEvents(examId));
    }

    @Operation(summary = "考试成绩与行为分析")
    @GetMapping("/exams/{examId}/analytics")
    public ApiResponse<ExamAnalyticsVO> analytics(@PathVariable Long examId) {
        return ApiResponse.success(monitoringService.teacherAnalytics(examId));
    }
}
