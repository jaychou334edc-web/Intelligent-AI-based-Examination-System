import { apiRequest } from './client'

export interface AntiCheatEventPayload {
  eventType: string
  eventLevel?: 'info' | 'warning' | 'critical'
  eventData?: Record<string, unknown>
  clientTime?: string
}

export interface EventRecorded {
  eventId: number
  status: string
  recordedAt: string
}

export interface AntiCheatEvent {
  id: number
  userId: number
  studentName: string
  examId: number
  examTitle: string
  eventType: string
  eventLevel: string
  eventData: Record<string, unknown>
  clientTime?: string
  createdAt: string
}

export interface ScoreBucket {
  label: string
  count: number
}

export interface QuestionAccuracy {
  questionId: number
  questionType: string
  stem: string
  maxScore: number
  averageScore: number
  accuracyRate: number
}

export interface DifficultyStats {
  difficulty: string
  questionCount: number
  averageScore: number
  accuracyRate: number
}

export interface KnowledgePointStats {
  knowledgePoint: string
  questionCount: number
  averageScore: number
  accuracyRate: number
}

export interface EventCount {
  eventType: string
  count: number
}

export interface ExamAnalytics {
  examId: number
  examTitle: string
  participantCount: number
  submittedCount: number
  averageScore: number
  maxScore: number
  minScore: number
  passRate: number
  scoreDistribution: ScoreBucket[]
  questionAccuracy: QuestionAccuracy[]
  difficultyStats: DifficultyStats[]
  knowledgePointStats: KnowledgePointStats[]
  eventCounts: EventCount[]
}

export function recordAntiCheatEvent(examId: number, payload: AntiCheatEventPayload) {
  return apiRequest<EventRecorded>(`/api/student/monitoring/exams/${examId}/events`, {
    method: 'POST',
    body: JSON.stringify({
      ...payload,
      clientTime: payload.clientTime ?? new Date().toISOString(),
    }),
  })
}

export function getTeacherExamEvents(examId: number) {
  return apiRequest<AntiCheatEvent[]>(`/api/teacher/monitoring/exams/${examId}/events`)
}

export function getTeacherExamAnalytics(examId: number) {
  return apiRequest<ExamAnalytics>(`/api/teacher/monitoring/exams/${examId}/analytics`)
}
