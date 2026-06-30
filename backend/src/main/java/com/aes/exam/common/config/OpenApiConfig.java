package com.aes.exam.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("智能化在线考试系统 API")
                .version("0.1.5")
                .description("学校机房在线考试系统后端接口文档"));
    }
}
