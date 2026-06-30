import { apiRequest, getStoredToken } from './client'

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

export async function loadPaperImageUrl(paperId: number, imageId: string) {
  const token = getStoredToken()
  const response = await fetch(`/api/papers/${paperId}/images/${imageId}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!response.ok) {
    throw new Error(`图片加载失败：${response.status}`)
  }
  return URL.createObjectURL(await response.blob())
}
