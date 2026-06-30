package com.aes.exam.common.security;

import com.aes.exam.auth.service.AuthTokenService;
import com.aes.exam.auth.service.CurrentUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityContextFilter extends OncePerRequestFilter {

    private final AuthTokenService tokenService;
    private final CurrentUserService currentUserService;

    public SecurityContextFilter(AuthTokenService tokenService, CurrentUserService currentUserService) {
        this.tokenService = tokenService;
        this.currentUserService = currentUserService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            tokenService.resolveBearerToken(request)
                .flatMap(currentUserService::loadContextByToken)
                .ifPresent(SecurityContextHolder::set);
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clear();
        }
    }
}
