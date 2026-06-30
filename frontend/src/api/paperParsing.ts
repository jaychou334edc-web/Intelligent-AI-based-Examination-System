import { apiRequest } from './client'

export interface QuestionOption {
  key: string
  text: string
}

export interface ReviewQuestion {
  parsedQuestionId?: number
  questionType: string
  stem: string
  options: QuestionOption[]
  answer: string
  analysis: string
  score: number
  knowledgePoint: string
  difficulty: string
  reviewStatus?: string
  reviewComment?: string
}

export interface ParseResult {
  paperId: number
  parseJobId?: number
  status: string
  rawText: string
  questions: ReviewQuestion[]
}

export interface ImportResult {
  paperId: number
  importedCount: number
}

export function parsePaper(paperId: number) {
  return apiRequest<ParseResult>('/api/ai/parse-paper', {
    method: 'POST',
    body: JSON.stringify({ paperId }),
  })
}

export function getParseResult(paperId: number) {
  return apiRequest<ParseResult>(`/api/ai/parse-result/${paperId}`)
}

export function importQuestions(paperId: number, questions: ReviewQuestion[]) {
  return apiRequest<ImportResult>('/api/questions/import', {
    method: 'POST',
    body: JSON.stringify({ paperId, questions }),
  })
}
