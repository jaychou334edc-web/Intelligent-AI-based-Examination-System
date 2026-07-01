package com.aes.exam.academic;

import com.aes.exam.academic.dto.CreateCourseRequest;
import com.aes.exam.academic.dto.CreateTeachingClassRequest;
import com.aes.exam.academic.dto.UpdateClassStudentsRequest;
import com.aes.exam.academic.service.AcademicService;
import com.aes.exam.academic.vo.AcademicOverviewVO;
import com.aes.exam.academic.vo.ClassStudentVO;
import com.aes.exam.academic.vo.CourseVO;
import com.aes.exam.academic.vo.TeachingClassVO;
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

@Tag(name = "课程班级")
@RestController
@RequestMapping("/api/academic")
public class AcademicController {

    private final AcademicService academicService;

    public AcademicController(AcademicService academicService) {
        this.academicService = academicService;
    }

    @Operation(summary = "课程班级总览")
    @GetMapping("/overview")
    public ApiResponse<AcademicOverviewVO> overview() {
        return ApiResponse.success(academicService.overview());
    }

    @Operation(summary = "班级列表")
    @GetMapping("/classes")
    public ApiResponse<List<TeachingClassVO>> classes() {
        return ApiResponse.success(academicService.classes());
    }

    @Operation(summary = "班级学生列表")
    @GetMapping("/classes/{classId}/students")
    public ApiResponse<List<ClassStudentVO>> classStudents(@PathVariable Long classId) {
        return ApiResponse.success(academicService.classStudents(classId));
    }

    @Operation(summary = "创建课程")
    @PostMapping("/courses")
    public ApiResponse<CourseVO> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return ApiResponse.success(academicService.createCourse(request));
    }

    @Operation(summary = "创建班级")
    @PostMapping("/classes")
    public ApiResponse<TeachingClassVO> createClass(@Valid @RequestBody CreateTeachingClassRequest request) {
        return ApiResponse.success(academicService.createClass(request));
    }

    @Operation(summary = "更新班级学生")
    @PutMapping("/classes/{classId}/students")
    public ApiResponse<List<ClassStudentVO>> updateClassStudents(
        @PathVariable Long classId,
        @Valid @RequestBody UpdateClassStudentsRequest request
    ) {
        return ApiResponse.success(academicService.updateClassStudents(classId, request));
    }
}
