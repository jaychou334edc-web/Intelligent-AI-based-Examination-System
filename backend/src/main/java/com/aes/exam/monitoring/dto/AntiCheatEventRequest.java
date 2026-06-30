package com.aes.exam.monitoring.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Map;

public record AntiCheatEventRequest(
    @NotBlank String eventType,
    String eventLevel,
    Map<String, Object> eventData,
    LocalDateTime clientTime
) {
}
