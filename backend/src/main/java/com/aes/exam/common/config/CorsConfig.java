package com.aes.exam.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final AesProperties properties;

    public CorsConfig(AesProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        AesProperties.CorsProperties cors = properties.getCors();
        registry.addMapping("/api/**")
            .allowedOrigins(cors.getAllowedOrigins().toArray(String[]::new))
            .allowedMethods(cors.getAllowedMethods().toArray(String[]::new))
            .allowedHeaders(cors.getAllowedHeaders().toArray(String[]::new))
            .exposedHeaders(cors.getExposedHeaders().toArray(String[]::new))
            .allowCredentials(cors.isAllowCredentials())
            .maxAge(cors.getMaxAge());
    }
}
