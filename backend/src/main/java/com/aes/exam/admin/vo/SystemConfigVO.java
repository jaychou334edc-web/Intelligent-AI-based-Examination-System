package com.aes.exam.admin.vo;

public record SystemConfigVO(
    String schoolName,
    String uploadDir,
    String databaseUrl,
    Boolean flywayEnabled,
    String aiModel,
    Boolean aiMockEnabled,
    Boolean aiFallbackEnabled,
    Boolean deepSeekConfigured,
    Integer tokenTtlHours,
    String activeProfile
) {
}
