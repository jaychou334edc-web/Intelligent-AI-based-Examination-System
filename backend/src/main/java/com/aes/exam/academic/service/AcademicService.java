package com.aes.exam.academic.service;

import com.aes.exam.academic.dto.CreateCourseRequest;
import com.aes.exam.academic.dto.CreateTeachingClassRequest;
import com.aes.exam.academic.dto.UpdateClassStudentsRequest;
import com.aes.exam.academic.repository.AcademicRepository;
import com.aes.exam.academic.vo.AcademicOverviewVO;
import com.aes.exam.academic.vo.ClassStudentVO;
import com.aes.exam.academic.vo.CourseVO;
import com.aes.exam.academic.vo.TeachingClassVO;
import com.aes.exam.auth.UserRole;
import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.exception.BusinessException;
import com.aes.exam.common.security.SecurityContext;
import com.aes.exam.common.security.SecurityContextHolder;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicService {

    private final AcademicRepository academicRepository;

    public AcademicService(AcademicRepository academicRepository) {
        this.academicRepository = academicRepository;
    }

    public AcademicOverviewVO overview() {
        requireLogin();
        return new AcademicOverviewVO(
            academicRepository.findCourses(),
            academicRepository.findClasses(),
            academicRepository.findAvailableStudents()
        );
    }

    public List<TeachingClassVO> classes() {
        requireLogin();
        return academicRepository.findClasses();
    }

    public List<ClassStudentVO> classStudents(Long classId) {
        requireLogin();
        ensureClassExists(classId);
        return academicRepository.findStudentsByClass(classId);
    }

    @Transactional
    public CourseVO createCourse(CreateCourseRequest request) {
        SecurityContext context = requireAdmin();
        Long courseId = academicRepository.createCourse(request, context.userId());
        return academicRepository.findCourses()
            .stream()
            .filter(course -> course.id().equals(courseId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "课程创建失败"));
    }

    @Transactional
    public TeachingClassVO createClass(CreateTeachingClassRequest request) {
        SecurityContext context = requireAdmin();
        Long classId = academicRepository.createClass(request, context.userId());
        return academicRepository.findClassById(classId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "班级创建失败"));
    }

    @Transactional
    public List<ClassStudentVO> updateClassStudents(Long classId, UpdateClassStudentsRequest request) {
        SecurityContext context = requireAdmin();
        ensureClassExists(classId);
        academicRepository.replaceClassStudents(classId, request.studentIds(), context.userId());
        return academicRepository.findStudentsByClass(classId);
    }

    public TeachingClassVO requireClass(Long classId) {
        return academicRepository.findClassById(classId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "班级不存在"));
    }

    public int countClassStudents(Long classId) {
        return academicRepository.countClassStudents(classId);
    }

    private void ensureClassExists(Long classId) {
        requireClass(classId);
    }

    private SecurityContext requireLogin() {
        SecurityContext context = SecurityContextHolder.current();
        if (context == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return context;
    }

    private SecurityContext requireAdmin() {
        SecurityContext context = requireLogin();
        if (context.role() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return context;
    }
}
