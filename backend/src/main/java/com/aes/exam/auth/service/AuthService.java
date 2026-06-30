package com.aes.exam.auth.service;

import com.aes.exam.auth.UserStatus;
import com.aes.exam.auth.dto.LoginRequest;
import com.aes.exam.auth.entity.UserEntity;
import com.aes.exam.auth.repository.LoginSessionRepository;
import com.aes.exam.auth.repository.UserRepository;
import com.aes.exam.auth.vo.CurrentUserVO;
import com.aes.exam.auth.vo.LoginResponse;
import com.aes.exam.common.config.AesProperties;
import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.AuthException;
import com.aes.exam.common.security.SecurityContext;
import com.aes.exam.common.security.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AesProperties properties;
    private final AuthTokenService tokenService;
    private final PasswordService passwordService;
    private final UserRepository userRepository;
    private final LoginSessionRepository sessionRepository;

    public AuthService(
        AesProperties properties,
        AuthTokenService tokenService,
        PasswordService passwordService,
        UserRepository userRepository,
        LoginSessionRepository sessionRepository
    ) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.passwordService = passwordService;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        UserEntity user = userRepository.findByUsername(request.username())
            .filter(value -> value.status() == UserStatus.ACTIVE)
            .filter(value -> passwordService.matches(request.password(), value.passwordHash()))
            .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));

        String token = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(token);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusHours(properties.getAuth().getTokenTtlHours());

        sessionRepository.create(user.id(), tokenHash, expiredAt, servletRequest.getHeader("User-Agent"), servletRequest.getRemoteAddr());
        userRepository.updateLastLoginAt(user.id(), now);

        return new LoginResponse(token, toCurrentUser(user));
    }

    @Transactional
    public void logout(String token) {
        sessionRepository.revoke(tokenService.hashToken(token), LocalDateTime.now());
    }

    public CurrentUserVO currentUser() {
        SecurityContext context = SecurityContextHolder.current();
        if (context == null) {
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }
        return new CurrentUserVO(context.userId(), context.username(), context.role(), context.realName());
    }

    private CurrentUserVO toCurrentUser(UserEntity user) {
        return new CurrentUserVO(user.id(), user.username(), user.role(), user.realName());
    }
}
