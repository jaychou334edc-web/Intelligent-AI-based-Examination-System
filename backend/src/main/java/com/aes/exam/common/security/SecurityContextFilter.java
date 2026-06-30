package com.aes.exam.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityContextFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String userId = request.getHeader(USER_ID_HEADER);
            String role = request.getHeader(USER_ROLE_HEADER);
            if (StringUtils.hasText(userId) && StringUtils.hasText(role)) {
                SecurityContextHolder.set(new SecurityContext(userId, role));
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clear();
        }
    }
}
