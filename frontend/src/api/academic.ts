import { apiRequest } from './client'

export interface Course {
  id: number
  name: string
  code?: string
  description?: string
  teacherId?: number
  teacherName?: string
  status: string
  classCount: number
  studentCount: number
  createdAt: string
}

export interface TeachingClass {
  id: number
  courseId: number
  courseName: string
  name: string
  grade?: string
  major?: string
  status: string
  studentCount: number
  createdAt: string
}

export interface ClassStudent {
  id: number
  username: string
  realName?: string
  studentNumber?: string
}

export interface AcademicOverview {
  courses: Course[]
  classes: TeachingClass[]
  students: ClassStudent[]
}

export interface CreateCoursePayload {
  name: string
  code?: string
  description?: string
  teacherId?: number
}

export interface CreateClassPayload {
  courseId: number
  name: string
  grade?: string
  major?: string
  studentIds: number[]
}

export function getAcademicOverview() {
  return apiRequest<AcademicOverview>('/api/academic/overview')
}

export function getAcademicClasses() {
  return apiRequest<TeachingClass[]>('/api/academic/classes')
}

export function getClassStudents(classId: number) {
  return apiRequest<ClassStudent[]>(`/api/academic/classes/${classId}/students`)
}

export function createCourse(payload: CreateCoursePayload) {
  return apiRequest<Course>('/api/academic/courses', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createTeachingClass(payload: CreateClassPayload) {
  return apiRequest<TeachingClass>('/api/academic/classes', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateClassStudents(classId: number, studentIds: number[]) {
  return apiRequest<ClassStudent[]>(`/api/academic/classes/${classId}/students`, {
    method: 'PUT',
    body: JSON.stringify({ studentIds }),
  })
}
