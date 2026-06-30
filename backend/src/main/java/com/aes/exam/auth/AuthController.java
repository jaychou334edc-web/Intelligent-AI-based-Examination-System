package com.aes.exam.auth;

import com.aes.exam.auth.dto.LoginRequest;
import com.aes.exam.auth.service.AuthService;
import com.aes.exam.auth.service.AuthTokenService;
import com.aes.exam.auth.vo.CurrentUserVO;
import com.aes.exam.auth.vo.LoginResponse;
import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.AuthException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthTokenService tokenService;

    public AuthController(AuthService authService, AuthTokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, servletRequest));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = tokenService.resolveBearerToken(request)
            .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED));
        authService.logout(token);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取当前用户")
    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> me() {
        return ApiResponse.success(authService.currentUser());
    }
}
