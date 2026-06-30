package com.aes.exam.auth;

import com.aes.exam.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "角色入口")
@RestController
public class RoleShellController {

    @GetMapping("/api/admin/shell")
    public ApiResponse<RoleShellResponse> adminShell() {
        return ApiResponse.success(new RoleShellResponse("admin", "管理员工作台"));
    }

    @GetMapping("/api/teacher/shell")
    public ApiResponse<RoleShellResponse> teacherShell() {
        return ApiResponse.success(new RoleShellResponse("teacher", "教师工作台"));
    }

    @GetMapping("/api/student/shell")
    public ApiResponse<RoleShellResponse> studentShell() {
        return ApiResponse.success(new RoleShellResponse("student", "学生考试入口"));
    }

    public record RoleShellResponse(String role, String title) {
    }
}
