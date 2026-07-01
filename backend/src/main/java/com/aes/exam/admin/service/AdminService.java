package com.aes.exam.admin.service;

import com.aes.exam.admin.dto.AdminCreateUserRequest;
import com.aes.exam.admin.dto.AdminResetPasswordRequest;
import com.aes.exam.admin.dto.AdminUpdateUserRequest;
import com.aes.exam.admin.repository.AdminRepository;
import com.aes.exam.admin.vo.AdminUserVO;
import com.aes.exam.admin.vo.LoginSessionAuditVO;
import com.aes.exam.admin.vo.SystemConfigVO;
import com.aes.exam.auth.UserRole;
import com.aes.exam.auth.UserStatus;
import com.aes.exam.auth.entity.UserEntity;
import com.aes.exam.auth.repository.UserRepository;
import com.aes.exam.auth.service.PasswordService;
import com.aes.exam.common.config.AesProperties;
import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.BusinessException;
import com.aes.exam.common.security.SecurityContext;
import com.aes.exam.common.security.SecurityContextHolder;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordService passwordService;
    private final AesProperties aesProperties;
    private final Environment environment;
    private final String databaseUrl;
    private final boolean flywayEnabled;

    public AdminService(
        UserRepository userRepository,
        AdminRepository adminRepository,
        PasswordService passwordService,
        AesProperties aesProperties,
        Environment environment,
        @Value("${spring.datasource.url}") String databaseUrl,
        @Value("${spring.flyway.enabled:true}") boolean flywayEnabled
    ) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.passwordService = passwordService;
        this.aesProperties = aesProperties;
        this.environment = environment;
        this.databaseUrl = databaseUrl;
        this.flywayEnabled = flywayEnabled;
    }

    public List<AdminUserVO> users() {
        return userRepository.findAll().stream().map(this::toVO).toList();
    }

    @Transactional
    public AdminUserVO createUser(AdminCreateUserRequest request) {
        userRepository.findByUsername(request.username().trim()).ifPresent(user -> {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户名已存在");
        });
        Long userId = userRepository.createUser(
            request.username().trim(),
            passwordService.hash(request.password()),
            UserRole.fromValue(request.role()),
            request.realName().trim()
        );
        return userRepository.findById(userId).map(this::toVO)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户创建失败"));
    }

    @Transactional
    public AdminUserVO updateUser(Long userId, AdminUpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        SecurityContext context = currentUser();
        if (context.userId().equals(userId) && UserStatus.DISABLED.value().equals(request.status())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能禁用当前登录管理员");
        }
        if (context.userId().equals(userId) && !UserRole.ADMIN.value().equals(request.role())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能修改当前登录管理员的角色");
        }
        userRepository.updateUser(
            user.id(),
            UserRole.fromValue(request.role()),
            UserStatus.fromValue(request.status()),
            request.realName().trim()
        );
        return userRepository.findById(userId).map(this::toVO)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    @Transactional
    public AdminUserVO resetPassword(Long userId, AdminResetPasswordRequest request) {
        userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        userRepository.updatePassword(userId, passwordService.hash(request.password()));
        return userRepository.findById(userId).map(this::toVO)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    public List<LoginSessionAuditVO> sessions() {
        return adminRepository.findRecentSessions(100);
    }

    public SystemConfigVO systemConfig() {
        return new SystemConfigVO(
            aesProperties.getSchoolName(),
            aesProperties.getUploadDir(),
            maskDatabaseUrl(databaseUrl),
            flywayEnabled,
            aesProperties.getAi().getModel(),
            aesProperties.getAi().isMockEnabled(),
            aesProperties.getAi().isFallbackToRuleParser(),
            StringUtils.hasText(aesProperties.getAi().getDeepseekApiKey()),
            aesProperties.getAuth().getTokenTtlHours(),
            String.join(",", Arrays.asList(environment.getActiveProfiles()))
        );
    }

    private AdminUserVO toVO(UserEntity user) {
        return new AdminUserVO(
            user.id(),
            user.username(),
            user.role().value(),
            user.status().value(),
            user.realName(),
            user.lastLoginAt()
        );
    }

    private String maskDatabaseUrl(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("password=[^&]+", "password=****");
    }

    private SecurityContext currentUser() {
        SecurityContext context = SecurityContextHolder.current();
        if (context == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return context;
    }
}
