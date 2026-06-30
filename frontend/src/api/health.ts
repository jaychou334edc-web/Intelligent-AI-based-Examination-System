export interface HealthResponse {
  status: string
  service: string
  timestamp: string
}

export async function getBackendHealth(): Promise<HealthResponse> {
  const response = await fetch('/api/health')

  if (!response.ok) {
    throw new Error(`Backend health request failed: ${response.status}`)
  }

  return response.json() as Promise<HealthResponse>
}
