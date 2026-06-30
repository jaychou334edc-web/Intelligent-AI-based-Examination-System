package com.aes.exam.common.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aes")
public class AesProperties {

    private String schoolName;
    private String uploadDir;
    private AuthProperties auth = new AuthProperties();
    private CorsProperties cors = new CorsProperties();
    private AiProperties ai = new AiProperties();

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

    public AuthProperties getAuth() {
        return auth;
    }

    public void setAuth(AuthProperties auth) {
        this.auth = auth;
    }

    public CorsProperties getCors() {
        return cors;
    }

    public void setCors(CorsProperties cors) {
        this.cors = cors;
    }

    public AiProperties getAi() {
        return ai;
    }

    public void setAi(AiProperties ai) {
        this.ai = ai;
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

    public static class AuthProperties {

        private int tokenTtlHours = 8;
        private InitialAdminProperties initialAdmin = new InitialAdminProperties();
        private DevSeedProperties devSeed = new DevSeedProperties();

        public int getTokenTtlHours() {
            return tokenTtlHours;
        }

        public void setTokenTtlHours(int tokenTtlHours) {
            this.tokenTtlHours = tokenTtlHours;
        }

        public InitialAdminProperties getInitialAdmin() {
            return initialAdmin;
        }

        public void setInitialAdmin(InitialAdminProperties initialAdmin) {
            this.initialAdmin = initialAdmin;
        }

        public DevSeedProperties getDevSeed() {
            return devSeed;
        }

        public void setDevSeed(DevSeedProperties devSeed) {
            this.devSeed = devSeed;
        }
    }

    public static class InitialAdminProperties {

        private String username = "admin";
        private String password = "Admin@123456";
        private String realName = "系统管理员";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }
    }

    public static class DevSeedProperties {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class AiProperties {

        private String deepseekApiKey = "";
        private String deepseekBaseUrl = "https://api.deepseek.com";
        private boolean mockEnabled = true;
        private boolean fallbackToRuleParser = true;
        private String model = "deepseek-chat";

        public String getDeepseekApiKey() {
            return deepseekApiKey;
        }

        public void setDeepseekApiKey(String deepseekApiKey) {
            this.deepseekApiKey = deepseekApiKey;
        }

        public String getDeepseekBaseUrl() {
            return deepseekBaseUrl;
        }

        public void setDeepseekBaseUrl(String deepseekBaseUrl) {
            this.deepseekBaseUrl = deepseekBaseUrl;
        }

        public boolean isMockEnabled() {
            return mockEnabled;
        }

        public void setMockEnabled(boolean mockEnabled) {
            this.mockEnabled = mockEnabled;
        }

        public boolean isFallbackToRuleParser() {
            return fallbackToRuleParser;
        }

        public void setFallbackToRuleParser(boolean fallbackToRuleParser) {
            this.fallbackToRuleParser = fallbackToRuleParser;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
