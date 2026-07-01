package com.aes.exam.admin;

import com.aes.exam.admin.dto.AdminCreateUserRequest;
import com.aes.exam.admin.dto.AdminResetPasswordRequest;
import com.aes.exam.admin.dto.AdminUpdateUserRequest;
import com.aes.exam.admin.service.AdminService;
import com.aes.exam.admin.vo.AdminUserVO;
import com.aes.exam.admin.vo.LoginSessionAuditVO;
import com.aes.exam.admin.vo.SystemConfigVO;
import com.aes.exam.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员后台")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public ApiResponse<List<AdminUserVO>> users() {
        return ApiResponse.success(adminService.users());
    }

    @Operation(summary = "创建用户")
    @PostMapping("/users")
    public ApiResponse<AdminUserVO> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return ApiResponse.success(adminService.createUser(request));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/users/{userId}")
    public ApiResponse<AdminUserVO> updateUser(
        @PathVariable Long userId,
        @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        return ApiResponse.success(adminService.updateUser(userId, request));
    }

    @Operation(summary = "重置密码")
    @PostMapping("/users/{userId}/password")
    public ApiResponse<AdminUserVO> resetPassword(
        @PathVariable Long userId,
        @Valid @RequestBody AdminResetPasswordRequest request
    ) {
        return ApiResponse.success(adminService.resetPassword(userId, request));
    }

    @Operation(summary = "登录会话审计")
    @GetMapping("/audit/sessions")
    public ApiResponse<List<LoginSessionAuditVO>> sessions() {
        return ApiResponse.success(adminService.sessions());
    }

    @Operation(summary = "系统配置摘要")
    @GetMapping("/config")
    public ApiResponse<SystemConfigVO> systemConfig() {
        return ApiResponse.success(adminService.systemConfig());
    }
}
