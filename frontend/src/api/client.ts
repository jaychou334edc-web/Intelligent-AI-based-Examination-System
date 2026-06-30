export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  requestId: string
  timestamp: string
}

const TOKEN_KEY = 'aes_auth_token'

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setStoredToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearStoredToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const token = getStoredToken()

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(path, {
    ...options,
    headers,
  })

  const result = (await response.json()) as ApiResponse<T>

  if (!response.ok || !result.success) {
    throw new Error(result.message || `${result.code}: ${response.status}`)
  }

  return result.data
}
