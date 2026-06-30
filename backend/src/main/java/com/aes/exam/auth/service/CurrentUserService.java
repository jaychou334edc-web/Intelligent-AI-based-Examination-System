package com.aes.exam.auth.service;

import com.aes.exam.auth.UserStatus;
import com.aes.exam.auth.entity.LoginSessionEntity;
import com.aes.exam.auth.repository.LoginSessionRepository;
import com.aes.exam.auth.repository.UserRepository;
import com.aes.exam.common.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final AuthTokenService tokenService;
    private final LoginSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public CurrentUserService(
        AuthTokenService tokenService,
        LoginSessionRepository sessionRepository,
        UserRepository userRepository
    ) {
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    public Optional<SecurityContext> loadContextByToken(String token) {
        String tokenHash = tokenService.hashToken(token);
        Optional<LoginSessionEntity> session = sessionRepository.findByTokenHash(tokenHash)
            .filter(value -> value.isActive(LocalDateTime.now()));

        if (session.isEmpty()) {
            return Optional.empty();
        }

        return userRepository.findById(session.get().userId())
            .filter(user -> user.status() == UserStatus.ACTIVE)
            .map(user -> new SecurityContext(user.id(), user.username(), user.role(), user.realName()));
    }
}
