package com.aes.exam.common.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aes")
public class AesProperties {

    private String schoolName;
    private String uploadDir;
    private CorsProperties cors = new CorsProperties();

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public CorsProperties getCors() {
        return cors;
    }

    public void setCors(CorsProperties cors) {
        this.cors = cors;
    }

    public static class CorsProperties {

        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = new ArrayList<>();
        private List<String> allowedHeaders = new ArrayList<>();
        private List<String> exposedHeaders = new ArrayList<>();
        private boolean allowCredentials = true;
        private long maxAge = 3600;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }
}
