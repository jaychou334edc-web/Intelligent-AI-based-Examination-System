package com.aes.exam.academic.vo;

import java.util.List;

public record AcademicOverviewVO(
    List<CourseVO> courses,
    List<TeachingClassVO> classes,
    List<ClassStudentVO> students
) {
}
