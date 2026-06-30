import { apiRequest } from './client'
import type { QuestionOption } from './paperParsing'

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
