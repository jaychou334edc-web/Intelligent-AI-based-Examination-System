export interface DashboardTask {
  id: string
  type: 'ai' | 'grading' | 'exam' | 'question'
  title: string
  meta: string
  target: string
}
