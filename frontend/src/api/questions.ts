import { apiRequest } from './client'
import type { QuestionOption, ReviewQuestion } from './paperParsing'

export interface QuestionBankItem {
  id: number
  sourcePaperId?: number
  questionType: string
  stem: string
  analysis: string
  score: number
  difficulty: string
  knowledgePoint: string
  answer: string
  options: QuestionOption[]
  createdAt: string
}

export function getRecentQuestions(limit = 100) {
  return apiRequest<QuestionBankItem[]>(`/api/questions?limit=${limit}`)
}

export function updateQuestion(questionId: number, payload: ReviewQuestion) {
  return apiRequest<QuestionBankItem>(`/api/questions/${questionId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteQuestion(questionId: number) {
  return apiRequest<void>(`/api/questions/${questionId}`, {
    method: 'DELETE',
  })
}
