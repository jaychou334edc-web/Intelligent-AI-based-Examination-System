package com.aes.exam.academic.repository;

import com.aes.exam.academic.dto.CreateCourseRequest;
import com.aes.exam.academic.dto.CreateTeachingClassRequest;
import com.aes.exam.academic.vo.ClassStudentVO;
import com.aes.exam.academic.vo.CourseVO;
import com.aes.exam.academic.vo.TeachingClassVO;
import java.util.List;
import java.util.Optional;

public interface AcademicRepository {

    Long createCourse(CreateCourseRequest request, Long currentUserId);

    Long createClass(CreateTeachingClassRequest request, Long currentUserId);

    List<CourseVO> findCourses();

    List<TeachingClassVO> findClasses();

    List<ClassStudentVO> findAvailableStudents();

    List<ClassStudentVO> findStudentsByClass(Long classId);

    Optional<TeachingClassVO> findClassById(Long classId);

    void replaceClassStudents(Long classId, List<Long> studentIds, Long currentUserId);

    int countClassStudents(Long classId);
}
