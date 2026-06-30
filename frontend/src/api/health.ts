export interface HealthResponse {
  status: string
  service: string
  timestamp: string
}

interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  requestId: string
  timestamp: string
}

export async function getBackendHealth(): Promise<HealthResponse> {
  const response = await fetch('/api/health')

  if (!response.ok) {
    throw new Error(`Backend health request failed: ${response.status}`)
  }

  const result = (await response.json()) as ApiResponse<HealthResponse>
  if (!result.success) {
    throw new Error(`${result.code}: ${result.message}`)
  }

  return result.data
}
