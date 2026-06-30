import { apiRequest } from './client'

export type UserRole = 'admin' | 'teacher' | 'student'

export interface CurrentUser {
  id: number
  username: string
  role: UserRole
  realName: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: CurrentUser
}

export function login(request: LoginRequest) {
  return apiRequest<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function logout() {
  return apiRequest<void>('/api/auth/logout', {
    method: 'POST',
  })
}

export function getCurrentUser() {
  return apiRequest<CurrentUser>('/api/auth/me')
}
