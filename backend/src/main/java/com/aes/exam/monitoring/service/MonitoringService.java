package com.aes.exam.monitoring.service;

import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.BusinessException;
import com.aes.exam.common.security.SecurityContext;
import com.aes.exam.common.security.SecurityContextHolder;
import com.aes.exam.monitoring.dto.AntiCheatEventRequest;
import com.aes.exam.monitoring.repository.MonitoringRepository;
import com.aes.exam.monitoring.vo.AntiCheatEventVO;
import com.aes.exam.monitoring.vo.EventRecordedVO;
import com.aes.exam.monitoring.vo.ExamAnalyticsVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MonitoringService {

    private static final Set<String> EVENT_TYPES = Set.of(
        "browser_blur",
        "tab_hidden",
        "fullscreen_exit",
        "copy_attempt",
        "paste_attempt",
        "page_refresh",
        "abnormal_disconnect",
        "network_offline",
        "network_online",
        "repeated_submit"
    );

    private static final Set<String> EVENT_LEVELS = Set.of("info", "warning", "critical");

    private final MonitoringRepository monitoringRepository;
    private final ObjectMapper objectMapper;

    public MonitoringService(MonitoringRepository monitoringRepository, ObjectMapper objectMapper) {
        this.monitoringRepository = monitoringRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EventRecordedVO recordStudentEvent(Long examId, AntiCheatEventRequest request) {
        SecurityContext context = currentUser();
        if (!monitoringRepository.isStudentAssignedToExam(examId, context.userId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "考试不存在或未分配给当前学生");
        }
        String eventType = normalize(request.eventType());
        if (!EVENT_TYPES.contains(eventType)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "不支持的考试行为事件类型");
        }
        String eventLevel = normalize(StringUtils.hasText(request.eventLevel()) ? request.eventLevel() : "warning");
        if (!EVENT_LEVELS.contains(eventLevel)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "不支持的考试行为事件级别");
        }
        return monitoringRepository.recordEvent(
            examId,
            context.userId(),
            eventType,
            eventLevel,
            eventDataJson(request.eventData()),
            request.clientTime()
        );
    }

    public List<AntiCheatEventVO> teacherEvents(Long examId) {
        SecurityContext context = currentUser();
        ensureTeacherOwnsExam(examId, context.userId());
        return monitoringRepository.findEventsForTeacher(examId, context.userId());
    }

    public ExamAnalyticsVO teacherAnalytics(Long examId) {
        SecurityContext context = currentUser();
        ensureTeacherOwnsExam(examId, context.userId());
        return monitoringRepository.findAnalyticsForTeacher(examId, context.userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "考试不存在"));
    }

    private void ensureTeacherOwnsExam(Long examId, Long teacherId) {
        if (!monitoringRepository.isTeacherExamOwner(examId, teacherId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "考试不存在");
        }
    }

    private String eventDataJson(Map<String, Object> eventData) {
        Map<String, Object> payload = eventData == null ? monitoringRepository.emptyEventData() : eventData;
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "事件数据格式不正确");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private SecurityContext currentUser() {
        SecurityContext context = SecurityContextHolder.current();
        if (context == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return context;
    }
}
