import { apiRequest } from './client'
import type { QuestionOption } from './paperParsing'

export interface GradingAnswer {
  questionId: number
  sourcePaperId?: number
  questionType: string
  stem: string
  options: QuestionOption[]
  maxScore: number
  correctAnswer?: string
  studentAnswer?: string
  isCorrect?: boolean
  autoScore?: number
  manualScore?: number
  finalScore?: number
  gradingStatus: string
  teacherComment?: string
}

export interface SubmissionSummary {
  submissionId: number
  examId: number
  examTitle: string
  studentId: number
  studentName: string
  submissionStatus: string
  totalScore?: number
  maxScore: number
  submittedAt?: string
  gradedAt?: string
}

export interface SubmissionGrading extends SubmissionSummary {
  answers: GradingAnswer[]
}

export function getTeacherSubmissions() {
  return apiRequest<SubmissionSummary[]>('/api/teacher/grading/submissions')
}

export function getTeacherSubmission(submissionId: number) {
  return apiRequest<SubmissionGrading>(`/api/teacher/grading/submissions/${submissionId}`)
}

export function gradeTeacherAnswer(submissionId: number, questionId: number, score: number, teacherComment: string) {
  return apiRequest<SubmissionGrading>(`/api/teacher/grading/submissions/${submissionId}/answers`, {
    method: 'POST',
    body: JSON.stringify({ questionId, score, teacherComment }),
  })
}

export function getStudentResults() {
  return apiRequest<SubmissionSummary[]>('/api/student/results')
}

export function getStudentResult(submissionId: number) {
  return apiRequest<SubmissionGrading>(`/api/student/results/${submissionId}`)
}
