import { apiRequest } from './client'
import type { QuestionOption } from './paperParsing'

export interface ExamQuestion {
  id: number
  sourcePaperId?: number
  questionType: string
  stem: string
  analysis?: string
  score: number
  answer?: string
  options: QuestionOption[]
  sortOrder: number
  savedAnswer?: string
}

export interface ExamSummary {
  id: number
  title: string
  description?: string
  durationMinutes: number
  status: string
  questionCount: number
  totalScore: number
  publishedAt?: string
  createdAt: string
  submissionStatus?: string
}

export interface ExamDetail extends ExamSummary {
  questions: ExamQuestion[]
  submissionId?: number
  submissionStartedAt?: string
}

export interface CreateExamPayload {
  title: string
  description?: string
  durationMinutes: number
}

export interface SaveAnswerPayload {
  questionId: number
  answer: string
}

export function createTeacherExam(payload: CreateExamPayload) {
  return apiRequest<ExamDetail>('/api/teacher/exams', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getTeacherExams() {
  return apiRequest<ExamSummary[]>('/api/teacher/exams')
}

export function getTeacherExam(examId: number) {
  return apiRequest<ExamDetail>(`/api/teacher/exams/${examId}`)
}

export function updateTeacherExamQuestions(examId: number, questionIds: number[]) {
  return apiRequest<ExamDetail>(`/api/teacher/exams/${examId}/questions`, {
    method: 'POST',
    body: JSON.stringify({ questionIds }),
  })
}

export function publishTeacherExam(examId: number) {
  return apiRequest<ExamDetail>(`/api/teacher/exams/${examId}/publish`, {
    method: 'POST',
  })
}

export function getStudentExams() {
  return apiRequest<ExamSummary[]>('/api/student/exams')
}

export function getStudentExam(examId: number) {
  return apiRequest<ExamDetail>(`/api/student/exams/${examId}`)
}

export function saveStudentAnswer(examId: number, payload: SaveAnswerPayload) {
  return apiRequest(`/api/student/exams/${examId}/answers`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function submitStudentExam(examId: number, answers: SaveAnswerPayload[]) {
  return apiRequest(`/api/student/exams/${examId}/submit`, {
    method: 'POST',
    body: JSON.stringify({ answers }),
  })
}
