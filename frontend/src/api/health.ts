import { apiRequest } from './client'

export interface HealthResponse {
  status: string
  service: string
  timestamp: string
}

export async function getBackendHealth(): Promise<HealthResponse> {
  return apiRequest<HealthResponse>('/api/health')
}
