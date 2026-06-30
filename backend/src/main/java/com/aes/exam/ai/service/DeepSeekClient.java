package com.aes.exam.ai.service;

import com.aes.exam.common.config.AesProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class DeepSeekClient {

    private final AesProperties properties;
    private final WebClient webClient;

    public DeepSeekClient(AesProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
    }

    public String parsePaper(String systemPrompt, String prompt) {
        if (!StringUtils.hasText(properties.getAi().getDeepseekApiKey())) {
            throw new IllegalStateException("DeepSeek API Key 未配置");
        }

        ChatRequest request = new ChatRequest(
            properties.getAi().getModel(),
            List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", prompt)
            ),
            new ResponseFormat("json_object"),
            0.1
        );

        ChatResponse response = webClient.post()
            .uri(properties.getAi().getDeepseekBaseUrl() + "/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .headers(headers -> headers.setBearerAuth(properties.getAi().getDeepseekApiKey()))
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse.class)
            .block(Duration.ofSeconds(120));

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("DeepSeek 未返回解析结果");
        }
        return response.choices().get(0).message().content();
    }

    public record ChatRequest(String model, List<ChatMessage> messages, ResponseFormat response_format, double temperature) {
    }

    public record ChatMessage(String role, String content) {
    }

    public record ResponseFormat(String type) {
    }

    public record ChatResponse(List<Choice> choices) {
    }

    public record Choice(ChatMessage message) {
    }
}
