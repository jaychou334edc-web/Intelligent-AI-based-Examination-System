package com.aes.exam.health;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", "AI Examination System backend", OffsetDateTime.now(ZoneOffset.UTC));
    }
}
