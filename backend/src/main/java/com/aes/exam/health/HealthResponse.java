package com.aes.exam.health;

import java.time.OffsetDateTime;

public record HealthResponse(String status, String service, OffsetDateTime timestamp) {
}
