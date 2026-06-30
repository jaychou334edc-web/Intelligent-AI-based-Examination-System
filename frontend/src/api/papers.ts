import { apiRequest } from './client'

export interface Paper {
  id: number
  title: string
  fileName: string
  fileSize: number
  parseStatus: string
  uploadTime: string
}

export function uploadPaper(file: File, title: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (title) {
    formData.append('title', title)
  }

  return apiRequest<Paper>('/api/papers', {
    method: 'POST',
    body: formData,
  })
}
