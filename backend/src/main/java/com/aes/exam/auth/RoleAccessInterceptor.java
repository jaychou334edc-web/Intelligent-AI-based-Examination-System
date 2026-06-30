package com.aes.exam.auth;

import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.AuthException;
import com.aes.exam.common.security.SecurityContext;
import com.aes.exam.common.security.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        UserRole requiredRole = requiredRole(path);
        boolean protectedAuthPath = path.equals("/api/auth/me") || path.equals("/api/auth/logout");

        if (requiredRole == null && !protectedAuthPath) {
            return true;
        }

        SecurityContext context = SecurityContextHolder.current();
        if (context == null) {
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }

        if (requiredRole != null && context.role() != requiredRole) {
            throw new AuthException(ErrorCode.FORBIDDEN);
        }

        return true;
    }

    private UserRole requiredRole(String path) {
        if (path.startsWith("/api/admin/")) {
            return UserRole.ADMIN;
        }
        if (path.startsWith("/api/teacher/")) {
            return UserRole.TEACHER;
        }
        if (path.startsWith("/api/student/")) {
            return UserRole.STUDENT;
        }
        if (path.startsWith("/api/papers") || path.startsWith("/api/ai/") || path.startsWith("/api/questions")) {
            return UserRole.TEACHER;
        }
        return null;
    }
}
