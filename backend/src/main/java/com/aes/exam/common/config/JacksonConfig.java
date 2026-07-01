package com.aes.exam.common.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer jsonTimeCustomizer() {
        return builder -> builder
            .timeZone(TimeZone.getTimeZone("Asia/Shanghai"))
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
